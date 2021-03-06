package lila.db

import scala.collection.generic.CanBuildFrom

import reactivemongo.api._
import reactivemongo.bson._

trait QueryBuilderExt { self: dsl =>

  final implicit class ExtendQueryBuilder[A](val b: dsl.QueryBuilder) {

    def skip(nb: Int) = b.options(b.options skip nb)

    def batch(nb: Int) = b.options(b.options batchSize nb)

    // like collect, but with stopOnError defaulting to false
    def gather[A, M[_]](upTo: Int, readPreference: ReadPreference = ReadPreference.primary)(implicit cbf: CanBuildFrom[M[_], A, M[A]], reader: BSONDocumentReader[A]): Fu[M[A]] =
      b.cursor[A](readPreference = readPreference).collect[M](upTo, Cursor.ContOnError[M[A]]())

    def list[A: BSONDocumentReader](): Fu[List[A]] =
      gather[A, List](Int.MaxValue)

    def list[A: BSONDocumentReader](limit: Int): Fu[List[A]] =
      gather[A, List](limit)

    def list[A: BSONDocumentReader](limit: Option[Int]): Fu[List[A]] =
      gather[A, List](limit | Int.MaxValue)

    def list[A: BSONDocumentReader](limit: Option[Int], readPreference: ReadPreference): Fu[List[A]] =
      gather[A, List](limit | Int.MaxValue, readPreference)

    def list[A: BSONDocumentReader](limit: Int, readPreference: ReadPreference): Fu[List[A]] =
      gather[A, List](limit, readPreference)

    def uno[A: BSONDocumentReader]: Fu[Option[A]] = uno[A](ReadPreference.primary)

    // like one, but with stopOnError defaulting to false
    def uno[A: BSONDocumentReader](readPreference: ReadPreference): Fu[Option[A]] =
      b.copy(options = b.options.batchSize(1))
        .cursor[A](readPreference = readPreference)
        .collect[Iterable](1, Cursor.ContOnError[Iterable[A]]())
        .dmap(_.headOption)
  }
}
