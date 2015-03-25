package tryout

import java.sql.Timestamp

import com.github.nscala_time.time.Imports._
import org.joda.time.Days
import org.squeryl.{Schema, Session, SessionFactory}
import org.squeryl.KeyedEntity
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.customtypes.DoubleField
import org.squeryl.dsl.{OneToMany, _}
import tryout.Domain2.Category.Category
import com.wix.accord._
import com.wix.accord.dsl._

object Domain2 extends org.squeryl.PrimitiveTypeMode {

  implicit val jodaTimeTEF = new NonPrimitiveJdbcMapper[Timestamp, DateTime, TTimestamp](timestampTEF, this) {
    def convertFromJdbc(t: Timestamp) = new DateTime(t)

    def convertToJdbc(t: DateTime) = new Timestamp(t.getMillis)
  }

  // these validations throw exceptions, not usable
  trait DomainValue[A] {
    self: Product1[Any] =>

    def label: String

    def validate(a: A): Unit

    def value: A

    validate(value)
  }

  class WeightInKilograms(v: Double) extends DoubleField(v) with DomainValue[Double] {
    def validate(d: Double) = assert(d > 0, "weight must be positive, got " + d)

    def label = "weight (in kilograms)"
  }

  case class Student(var name: String, var weight: WeightInKilograms) extends DbObject

  class DbObject extends KeyedEntity[Long] {
    val id: Long = 0
    val last_update = new Timestamp(System.currentTimeMillis())
  }

  case class Author(var firstName: String,
                    var lastName: String,
                    var email: Option[String] = None,
                    var full_content: Option[String] = None, var dob: DateTime = DateTime.now - 30.years) extends DbObject {
    lazy val books: OneToMany[Book] = SquerylConfiguration2.booksToAuthors.left(this)
    lazy val full_name = s"$firstName $lastName"
  }

  // enumeration just stores the id in the table
  object Category extends Enumeration {
    type Category = Value
    val Crime = Value(1, "Crime")
    val Fiction = Value(2, "Fiction")
    val Syfi = Value(3, "Syfi")
  }

  case class Book(var title: String, var authorId: Option[Long] = None, var read: Boolean = false, category: Option[Category] = None) extends DbObject {
    def this() = this("", Some(0), true, Some(Category.Crime))

    def author: Option[Author] = authorId.headOption match {
      case Some(i) => SquerylConfiguration2.authors.where(a => a.id === i).headOption
      case None => None
    }

    override def toString = s"$title [${author.getOrElse("No author")}]: ${if (read) "read" else "un-read"}"
  }


  case class Address(house: Int, street: String) extends DbObject

  implicit val addressValidator = validator[Address] { a =>
    a.house should be > 0
    a.street has size > 5
  }


  // validation https://github.com/squeryl/squeryl/blob/c55c300b2745ce6bb64c36b375bab8d87c1e626b/src/test/scala/org/squeryl/test/schooldb/SchoolDb.scala#L283
  // case class validation
  // https://github.com/davegurnell/validation or https://github.com/tim-group/eithervalidation


  object SquerylConfiguration2 extends Schema {
    val authors = table[Author]
    val books = table[Book]
    val students = table[Student]
    val addresses = table[Address]
    val booksToAuthors = oneToManyRelation(authors, books).via((a, b) => a.id === b.authorId)
    // printDdl
    inTransaction {
      try {
        drop
      } catch {
        case e: Throwable => println(s"$e")
      }
      try {
        create
      } catch {
        case e: Throwable => println(s"$e")
      }
    }

    override def callbacks = Seq(
      afterInsert[Address]
        map { a => validate(a) match {
        case Success => a
        case Failure(m) => println(m);a
        }
      }
    )

  }

}


import tryout.Domain2.SquerylConfiguration2._
import tryout.Domain2._

object SquerylTryOuts extends App {


  //Class.forName("org.h2.Driver")
  Class.forName("org.postgresql.Driver")
  SessionFactory.concreteFactory = Some(() =>
    Session.create(
      //java.sql.DriverManager.getConnection("jdbc:h2:~/~.h2.db", "sa", ""), new H2Adapter)
      java.sql.DriverManager.getConnection("jdbc:postgresql://localhost:5432/squeryl_tryout", "postgres", "postgres"), new PostgreSqlAdapter)
  )

  inTransaction {
    val jrrt: Author = authors.insert(Author("JRR", "Tolkien", None, None))
    println(s"${jrrt.full_name} is ${(jrrt.dob to DateTime.now).toPeriod.getYears} years old")
    println(s"${jrrt.full_name} is ${Days.daysBetween(jrrt.dob, DateTime.now).getDays} days old")
    books.insert(Book("The Lord of the Rings", Some(jrrt.id), read = false, Some(Category.Fiction)))
    val book = books.insert(Book("Anonymous script", None, read = false))
    books.where(_.title like "%Lord%").headOption match {
      case Some(b) => println(s"Found the book $b")
      case None => println("No book found")
    }
    //    book.author.assign(jrrt)
    println(book)
    book.authorId = Some(jrrt.id)
    book.save
    println(book)

  }
  Student("Bob", new WeightInKilograms(20))

  val a = Address(0, "abc")
  transaction(addresses.insert(a))

}
