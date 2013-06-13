package com.github.nuriaion.eventsourcedTodo

import scala.language.postfixOps

import akka.actor._
import akka.io.IO
import akka.util.Timeout
import akka.pattern._

import scala.concurrent.duration._

import org.eligosource.eventsourced.core._
import org.eligosource.eventsourced.journal.journalio.JournalioJournalProps

import java.io.File

import spray.routing.{HttpService}
import spray.can.Http

import spray.json._
import DefaultJsonProtocol._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

import spray.httpx.SprayJsonSupport._
import spray.httpx.marshalling.BasicMarshallers._
import spray.httpx.marshalling.MetaMarshallers._

import spray.httpx.unmarshalling.pimpHttpEntity
import spray.json.DefaultJsonProtocol
import spray.httpx.marshalling._
import spray.http._
import HttpCharsets._
import MediaTypes._


import spray.httpx.SprayJsonSupport._

import TodoCommands._
import TodoDataProtocol._

import ExecutionContext.Implicits.global

object TodoCommands {

  type TodoId = Int
  type EntryId = Int

  case class CreateTodo(name: String)
  case class GetTodo(id:TodoId)
  case class DeleteTodo(id:TodoId)
  type GetTodoResponse = Option[Todo]

  case class AddTodoEntry(id:TodoId, name: String)

  case class Todo(id:TodoId, name: String, entries: List[Entry])
  case class Entry(id: EntryId, name:String)

  object TodoDataProtocol {
    implicit val entryFormat = jsonFormat2(Entry)
    implicit val todoFormat = jsonFormat3(Todo)
    implicit val crateTodoFormat = jsonFormat1(CreateTodo)
  }
}


class TodoProcessor extends Actor with ActorLogging {
  this: Behavior  =>
  import TodoCommands._

  def store(id:Int, todos: Map[Int, Todo], c:Int, startup: Long):Receive = {
    case "foo" => {
      become(store(id, todos, c+1, startup))
      sender ! c
    }
    case CreateTodo(name) => {
      val todo = Todo(id, name, Nil)
      become(store(id+1, todos + (id -> todo), c, startup))
      sender ! todo
      if(id % 10000 == 0) println(s"id: $id, ${(System.nanoTime() - startup) / 1000000000.0}")
    }
    case GetTodo(todoId) => {
      sender ! todos.get(todoId)
    }
    case DeleteTodo(todoId) => {
      val res = todos.get(todoId)
      become(store(id, todos - todoId, c, startup))
      sender ! res
    }
    case AddTodoEntry(todoId, entryName) => {
      sender ! todos.get(todoId).map { (t: Todo) =>
        val t2 = t.copy(entries = t.entries :+ Entry(t.entries.size, entryName))
        val todos2 = todos + (todoId -> t2)
        become(store(id, todos2, c, startup))
        t2
      }
    }
    case m => println(s"HO? $m")
  }

  override def receive = store(0, Map.empty, 0, System.nanoTime)

}

trait DemoPath {
  this: HttpService =>

  import TodoCommands._
  import TodoDataProtocol._

  implicit val timeout = Timeout(1.seconds)

  def todoProcessor:ActorRef

  def swaggerUI = get {
    pathPrefix("swagger") {
      getFromBrowseableDirectory("swaggerDist")
    }
  }

  def swaggerDefinitions = {
    pathPrefix("swaggerDef") {
      getFromBrowseableDirectory("swagger_definitions")
    }
  }

  def todoRoute =
    post {
      path("todo") {
        entity(as[CreateTodo]) { ct =>
          complete {
            (todoProcessor ? Message(ct)).mapTo[Todo]
          }
        }
      }
    }~
    path("todo" / IntNumber) { todoListId =>
      get {
        rejectEmptyResponse {
          complete{
            ((todoProcessor ? GetTodo(todoListId)).mapTo[GetTodoResponse])
          }
        }
      } ~
      delete {
        complete{
          ((todoProcessor ? Message(DeleteTodo(todoListId))).mapTo[GetTodoResponse])
        }
      }
    }~
    path("todo" / IntNumber / "entry") { todoListId =>
      post {
        entity(as[String]) { name =>
          complete {
            (todoProcessor ? Message(AddTodoEntry(todoListId, name))).mapTo[GetTodoResponse]
          }
        }
      }
    } ~
    path("todo" / IntNumber / "entry" / IntNumber) { (todoListId, entryId) =>
      get {
        //println(s"GET $todoListId, $entryId")
        complete {
          (todoProcessor ? GetTodo(todoListId)).mapTo[GetTodoResponse].map{t =>
            //println(s"get entry $t")
            t.map (_.entries(entryId))
          }
        }
      }
    }

  def route = {
    todoRoute ~
    swaggerDefinitions ~
    swaggerUI
  }

}

class HttpService(val todoProcessor: ActorRef) extends Actor with HttpService with DemoPath {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(route)


}

object Main extends App {


  implicit val system = ActorSystem("on-spray-can")

  val journal: ActorRef = Journal(JournalioJournalProps(new File("target/example-3")))
  //val journal: ActorRef = Journal(InmemJournalProps())

  val extension: EventsourcingExtension = EventsourcingExtension(system, journal)

  val todoProcessor: ActorRef = extension.processorOf(Props(new TodoProcessor with Receiver with Eventsourced  {
    val id = 3
  }), Some("todo-processor"))

  val service = system.actorOf(Props(new HttpService(todoProcessor)), "demo-service")


  def time[A](a: => A) = {
    val now = System.nanoTime
    val result = a
    val micros = (System.nanoTime - now) / 1000
    println("%d microseconds".format(micros))
    println("%d s".format(micros / 1000000))
    result
  }

  println("start recover")
  time(extension.recover(waitAtMost = 5 minute))
  println("recovered")

  IO(Http) ! Http.Bind(service, "localhost", port = 8080)
}