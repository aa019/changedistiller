import java.util.*;

/**
* case : "change int to boolean , int to float and book to double"
*/

public class Parameter_Type_Change_Left{
  Book book1;

  public static void main (String[] args){

  }

  public void foo(boolean a, float b){

  }

  public void setBook(double book){
    book1 = book;
  }

}

class Book{
  String name;
  int numberOfPages;
  String writer;

  public Book(String name, int numberOfPages, String writer){
    this.name = name;
    this.numberOfPages = numberOfPages;
    this.writer = writer;
  }
}
