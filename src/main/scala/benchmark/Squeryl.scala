//import _root_.Domain.Author
package benchmark

import org.squeryl._
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.dsl._
//import PrimitiveTypeMode._
import java.sql.Timestamp

object Domain extends org.squeryl.PrimitiveTypeMode{

  class DbObject extends KeyedEntity[Long] {
    val id: Long = 0
    val last_update = new Timestamp(System.currentTimeMillis())
  }

  case class Award(var name: String) extends DbObject

  case class Author(var firstName: String,
               var lastName: String,
               var email: Option[String] = None,
               var full_content: Option[String] = None) extends DbObject {
    lazy val books: OneToMany[Book] = SquerylConfiguration.booksToAuthors.left(this)
    lazy val awards = SquerylConfiguration.awardPresentations.right(this)
    lazy val full_name = s"$firstName $lastName"
  }

  case class Book(var title: String, var authorId: Long, var read: Boolean = false) extends DbObject {
    lazy val author: ManyToOne[Author] = SquerylConfiguration.booksToAuthors.right(this)
  }
  class AwardPresentation(val award_id: Long, val author_id: Long) extends KeyedEntity[CompositeKey2[Long, Long]] {
    def id = compositeKey(award_id, author_id)
  }

  object SquerylConfiguration extends Schema {

    val authors = table[Author]
    val books = table[Book]
    val awards = table[Award]
    val booksToAuthors = oneToManyRelation(authors, books).via((a, b) => b.authorId === a.id)
    val awardPresentations = manyToManyRelation(awards, authors).via[AwardPresentation]((aw, au, ap) => (ap.author_id === au.id, aw.id === ap.award_id))
    // printDdl
    inTransaction {
      drop // don't do this in a production environment
      create
    }
  }
}

object Benchmarking {
  def time[R](description: String = "standard test", block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block // call-by-name
    val t1 = System.currentTimeMillis()
    println(s"Elapsed time for $description: " + (t1 - t0) + "ms")
    result
  }
}

import benchmark.Benchmarking._
import benchmark.Domain.SquerylConfiguration._
import benchmark.Domain._

object SquerylBenchMark extends App {
  //Class.forName("org.h2.Driver")
  Class.forName("org.postgresql.Driver")
  SessionFactory.concreteFactory = Some(() =>
    Session.create(
    //java.sql.DriverManager.getConnection("jdbc:h2:~/~.h2.db", "sa", ""), new H2Adapter)
    java.sql.DriverManager.getConnection("jdbc:postgresql://localhost:5432/benchmarking", "postgres", "postgres"), new PostgreSqlAdapter)
  )
  1 to 2 foreach { _ =>
    // Benchmarking insert
    inTransaction {
      time("insert statements", {
        1 to 50 foreach { n =>
          val jrrt = authors.insert(new Author("JRR", s"Tolkien$n"))
          authors.insert(new Author("Jane", "Austen"))
          authors.insert(new Author("Philip", "Pullman", None, Some( """ <xml>Test</xml> """)))
        }
        1 to 50 foreach { n =>
          val jrrt = authors.where(_.lastName === s"Tolkien$n").head
          val lord_of_the_rings = new Book("The Lord of the Rings", jrrt.id)
          books.insert(lord_of_the_rings)
          books.insert(new Book("Pride and Prejudice", jrrt.id))
          books.insert(new Book("His Dark Materials", jrrt.id))

          val manBookerPrice = Award("Man Booker Prize")
          val commonwealthBookPrize = Award("Commonwealth Book Prize")

          awards.insert(manBookerPrice)
          awards.insert(commonwealthBookPrize)

          awardPresentations.insert(new AwardPresentation(manBookerPrice.id, jrrt.id))
          jrrt.awards.associate(commonwealthBookPrize)
        }
      })
    }

    inTransaction {
      println(s"Number of authors in database: ${from(books)(select(_)).size}")
      time("select statements", {
        1 to 50 foreach { n =>
          val jrrt = authors.where(_.lastName === s"Tolkien$n").head
          jrrt.books.map(_.title).mkString(",")
          authors.allRows.map(_.full_name).mkString(",")
          books.where(_.title like ("Dark")).map(_.title).mkString(",")
          authors.where(_.lastName === "Pullmann").foreach(a => println(a.full_content))
        }
      })
    }
  }
}
