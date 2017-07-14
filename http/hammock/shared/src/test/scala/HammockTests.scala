/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle
package http


import cats.{MonadError, ~>}
import cats.instances.try_._
import cats.syntax.show._
import org.scalatest._
import _root_.hammock._
import _root_.hammock.free._
import _root_.hammock.free.algebra._

import freestyle.implicits._
import freestyle.http.hammock._
import freestyle.http.hammock.implicits._


class HammockTests extends WordSpec with Matchers {

  import stuff._

  implicit val interp = new InterpTrans {
    def trans[F[_]](implicit ME: MonadError[F, Throwable]): HttpRequestF ~> F = new (HttpRequestF ~> F) {
      def apply[A](req: HttpRequestF[A]): F[A] = req match {
        case req@Options(uri, headers) => doReq(req, Method.OPTIONS)(ME)
        case req@Get(uri, headers) => doReq(req, Method.GET)(ME)
        case req@Head(uri, headers) => doReq(req, Method.HEAD)(ME)
        case req@Post(uri, headers, body) => doReq(req, Method.POST)(ME)
        case req@Put(uri, headers, body) => doReq(req, Method.PUT)(ME)
        case req@Delete(uri, headers) => doReq(req, Method.DELETE)(ME)
        case req@Trace(uri, headers) => doReq(req, Method.TRACE)(ME)
      }
    }
  }

  private def doReq[F[_]](req: HttpRequestF[HttpResponse], method: Method)(implicit ME: MonadError[F, Throwable]): F[HttpResponse] = ME.catchNonFatal {
    HttpResponse(Status.OK, Map(), s"got a $method request to ${req.uri.show}")
  }

  "Hammock integration" should {

    val uri = Uri.unsafeParse("http://test.com")
    val headers = Map.empty[String, String]
    val body = None
    Seq(("options", app.hammock.options(uri, headers)),
      ("get", app.hammock.get(uri, headers)),
      ("head", app.hammock.head(uri, headers)),
      ("post", app.hammock.post(uri, headers, body)),
      ("put", app.hammock.put(uri, headers, body)),
      ("delete", app.hammock.delete(uri, headers)),
      ("trace", app.hammock.trace(uri, headers))) map {
      case (method, op) =>
        s"allow the use of $method requests" in {
          val program = for {
            _ <- app.nonHammock.x
            a <- op
            content <- FreeS.pure(HttpResponse.content.get(a))
          } yield content

          program.interpret[util.Try] shouldEqual util.Success(s"got a ${method.toUpperCase} request to ${uri.show}")
        }
    }


    "allow a HttpRequestIO program to be lifted inside a monadic flow" in {
      val program = for {
        a <- app.nonHammock.x
        b <- app.hammock.run(Ops.get(Uri.unsafeParse("http://test.com"), Map()))
        c <- FreeS.pure(HttpResponse.content.get(b))
      } yield c

      program.interpret[util.Try] shouldEqual util.Success(s"got a GET request to ${uri.show}")
    }

    "allow a HttpRequestIO program to be lifted to FreeS" in {
      val program = for {
        a <- app.nonHammock.x
        b <- Ops.get(Uri.unsafeParse("http://test.com"), Map()).liftFS[App.Op]
        c <- FreeS.pure(HttpResponse.content.get(b))
      } yield c

      program.interpret[util.Try] shouldEqual util.Success(s"got a GET request to ${uri.show}")
    }

    "allow a HttpRequestIO program to be lifted to FreeS.Par" in {
      val program = for {
        a <- app.nonHammock.x
        b <- Ops.get(Uri.unsafeParse("http://test.com"), Map()).liftFSPar[App.Op].freeS
        c <- FreeS.pure(HttpResponse.content.get(b))
      } yield c

      program.interpret[util.Try] shouldEqual util.Success(s"got a GET request to ${uri.show}")
    }

  }
  
}


object stuff {

  @free
  trait NonHammock {
    def x: FS[Int]
  }

  implicit def nonHammockHandler: NonHammock.Handler[util.Try] =
    new NonHammock.Handler[util.Try] {
      def x: util.Try[Int] = util.Success(42)
    }

  @module
  trait App {
    val nonHammock: NonHammock
    val hammock: HammockM
  }

  val app = App[App.Op]

}
