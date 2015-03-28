package services

object SpecialExceptions {

  class FetchedRecentlyException extends Exception
  class NotFoundException extends RuntimeException
  class UnexpectedException(msg: String) extends RuntimeException
  class TooManyRequestsException extends RuntimeException

}