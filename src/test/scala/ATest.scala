package com.github.nuriaion.eventsourcedTodo.test

import akka.actor.{Props, ActorSystem, ActorRef, Actor}
import java.io.File
import org.eligosource.eventsourced.core.{Receiver, Eventsourced, EventsourcingExtension, Journal}
import org.eligosource.eventsourced.journal.journalio.JournalioJournalProps
import org.specs2._
import org.specs2.specification.Grouped
import spray.testkit.Specs2RouteTest
import spray.routing.HttpService
import spray.http.StatusCodes._
import com.github.nuriaion.eventsourcedTodo._
import org.eligosource.eventsourced.journal.inmem.InmemJournalProps

import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.httpx.SprayJsonSupport._
import spray.http.HttpEntity

import spray.http.ContentTypes._

trait TodoProcessorContext {
  this: Specs2RouteTest =>

  val journal: ActorRef = Journal(InmemJournalProps())
  val extension: EventsourcingExtension = EventsourcingExtension(system, journal)

  val todoProcessor: ActorRef = extension.processorOf(Props(new TodoProcessor with Receiver with Eventsourced  {
    val id = 3
  }), Some("todo-write-processor"))

  def actorRefFactory = system // connect the DSL to the test ActorSystem
}

class DemoTest extends Specification with Specs2RouteTest with HttpService
  with DemoPath with TodoProcessorContext {

  def is = s2"""
The todo route
==============
  create new todo list        $createTodoList
  creat with some parameters  $createTodoListWithOptions
  get the todo list           $getTodoList
  get a not existing list     $getNoList
  delete a todoList           $deleteTodoList

  create a todolist entry     $createTodoListEntry
  get a todolist entry        $getTodoListEntry
  """





  import spray.json._
  import DefaultJsonProtocol._ // !!! IMPORTANT, else `convertTo` and `toJson` won't work

  import TodoCommands._
  import TodoDataProtocol._





  def createTodoList =
    postCreateTodo("ANewTodoList") ~> route ~> check {
      entityAs[Todo].name === "ANewTodoList"
    }

  def createTodoListWithOptions =
    Post("/todo?access_token=9127a608d52e6010e9b67a8e9ed30abeeea68f3c", jsonEntity(CreateTodo("SomeName").toJson)) ~> route ~> check {
      entityAs[Todo].name === "SomeName"
    }

  def getTodoList = {
    postCreateTodo("ATodo") ~> route ~> check {
      val createTodo = entityAs[Todo]
      getTodo(createTodo.id) ~> route ~> check {
        entityAs[Todo] === createTodo
      }
    }
  }


  def getNoList = {
    getTodo(9999) ~> sealRoute(route) ~> check {
      status === NotFound
    }
  }

  def deleteTodoList = {
    postCreateTodo("SomeTodoList") ~> route ~> check {
      val createTodo = entityAs[Todo]
      println("1")
      Delete(s"/todo/${createTodo.id}") ~> route ~> check {
        println("2")
        getTodo(createTodo.id) ~> sealRoute(route) ~> check {
          status === NotFound
        }
      }
    }
  }

  def createTodoListEntry = {
    postCreateTodo("SomeNewTodo") ~> route ~> check {
      val createdTodo = entityAs[Todo]
      Post(s"/todo/${createdTodo.id}/entry", "ATodoEntry") ~> route ~> check {
        entityAs[Todo] === createdTodo.copy(entries = List(Entry(0, "ATodoEntry")))
      }
    }
  }

  def getTodoListEntry = {
    postCreateTodo("SomeNewTodo") ~> route ~> check {
      val createdTodo = entityAs[Todo]
      println(s"GET Entry $createdTodo")
      Post(s"/todo/${createdTodo.id}/entry", "ATodoEntry") ~> route ~> check {
        val createdTodo2 = entityAs[Todo]
        println(s"GET Entry $createdTodo2")
        Get(s"/todo/${createdTodo.id}/entry/0") ~> route ~> check {
          println(s"e: $entity")
          entityAs[Entry] === Entry(0, "ATodoEntry")
        }
      }
    }
  }

  def jsonEntity(j:JsValue) = HttpEntity(`application/json`, j.prettyPrint)

  def postCreateTodo(name: String) = {
    Post("/todo", jsonEntity(CreateTodo(name).toJson))
  }

  def getTodo(id: TodoId) = Get("/todo/" + id)

  def postCreateEntry(id: TodoId, name: String) = {
    Post(s"/todo/${id}/entry", "ATodoEntry")
  }
}
