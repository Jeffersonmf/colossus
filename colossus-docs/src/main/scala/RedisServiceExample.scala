// #example
import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorSystem
import akka.util.ByteString
import colossus.core.{IOSystem, ServerContext}
import colossus.protocols.redis._
import colossus.protocols.redis.server.{Initializer, RedisServer, RequestHandler}
import colossus.service.Callback
import colossus.service.GenRequestHandler.PartialHandler

object RedisServiceExample extends App {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ioSystem: IOSystem       = IOSystem()

  val db = new ConcurrentHashMap[String, String]()

  RedisServer.start("example-server", 6379) { initContext =>
    new Initializer(initContext) {
      override def onConnect: RequestHandlerFactory = { serverContext =>
        new MyRequestHandler(serverContext, db)
      }
    }
  }
}

// #example2
class MyRequestHandler(context: ServerContext, db: ConcurrentHashMap[String, String]) extends RequestHandler(context) {
  override def handle: PartialHandler[Redis] = {
    case Command("GET", args) =>
      args match {
        case head +: _ =>
          Option(db.get(head.utf8String)) match {
            case Some(value) => Callback.successful(BulkReply(ByteString(value)))
            case None        => Callback.successful(NilReply)
          }
        case Nil =>
          Callback.successful(ErrorReply("ERR wrong number of arguments for 'get' command"))
      }

    case Command("SET", args) =>
      args match {
        case key +: value +: _ =>
          db.put(key.utf8String, value.utf8String)
          Callback.successful(StatusReply("OK"))
        case Nil =>
          Callback.successful(ErrorReply("ERR wrong number of arguments for 'set' command"))
      }
  }
}
// #example2
// #example
