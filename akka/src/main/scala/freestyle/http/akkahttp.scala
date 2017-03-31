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

import freestyle.{FreeS}
import _root_.akka.http.scaladsl.marshalling.ToEntityMarshaller
import cats.{~>, Monad}

package object akka {

  import freestyle._
  import freestyle.implicits._

  implicit def seqToEntityMarshaller[F[_], G[_], A](
      implicit NT: F ~> G,
      MonG: Monad[G],
      gem: ToEntityMarshaller[G[A]]): ToEntityMarshaller[FreeS[F, A]] =
    gem.compose((fs: FreeS[F, A]) => fs.exec[G])

  implicit def parToEntityMarshaller[F[_], G[_], A](
      implicit NT: F ~> G,
      MonG: Monad[G],
      gem: ToEntityMarshaller[G[A]]): ToEntityMarshaller[FreeS.Par[F, A]] =
    gem.compose((fp: FreeS.Par[F, A]) => FreeS.liftPar(fp).exec[G])

}
