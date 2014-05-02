package au.com.dius.pact.consumer

import au.com.dius.pact.model._


object PactSessionResults {
  val empty = PactSessionResults(Nil, Nil, Nil, Nil)
}

case class PactSessionResults(
    matched: List[Interaction], 
    almostMatched: List[PartialRequestMatch],
    missing: List[Interaction], 
    unexpected: List[Request]) {
  
  def addMatched(inter: Interaction) = copy(matched = inter :: matched)
  def addUnexpected(request: Request) = copy(unexpected = request :: unexpected)
  def addMissing(inters: Iterable[Interaction]) = copy(missing = inters ++: missing)
  def addAlmostMatched(partial: PartialRequestMatch) = copy(almostMatched = partial :: almostMatched)
  
  def allMatched: Boolean = missing.isEmpty && unexpected.isEmpty
}



object PactSession {
  val empty = PactSession(Set.empty, PactSessionResults.empty)
  def forPact(pact: Pact) = PactSession(pact.interactions.toSet, PactSessionResults.empty)
}

case class PactSession(expected: Set[Interaction], results: PactSessionResults) {
  private def matcher = RequestMatching(expected.toSeq)
  
  def receiveRequest(req: Request): (Response, PactSession) = matcher.matchInteraction(req) match {
    case FullRequestMatch(inter) => 
      (inter.response, recordMatched(inter))
      
    case p @ PartialRequestMatch(problems) => 
      (Response.invalidRequest(req), recordAlmostMatched(p))
      
    case RequestMismatch => 
      (Response.invalidRequest(req), recordUnexpected(req))
  }
  
  private def forgetAbout(req: Request): PactSession = 
    copy(expected = expected.filterNot(_.request == req))
  
  def recordUnexpected(req: Request): PactSession = 
    forgetAbout(req).copy(results = results addUnexpected req)
  
  def recordAlmostMatched(partial: PartialRequestMatch): PactSession = 
    copy(results = results addAlmostMatched partial)  
    
  def recordMatched(interaction: Interaction): PactSession = 
    forgetAbout(interaction.request).copy(results = results addMatched interaction)
  
  def withTheRestMissing: PactSession = PactSession(Set(), results addMissing expected)
}


