package org.hyperscala

case class Protocol private(scheme: String, description: String, rfc: String) {
  Protocol.schemeMap += scheme -> this

  override def toString = scheme
}

object Protocol {
  private var schemeMap = Map.empty[String, Protocol]

  val Aaa = Protocol("aaa", "Diameter Protocol","RFC6733")
  val Aaas = Protocol("aaas", "Diameter Protocol with Secure Transport", "RFC6733")
  val About = Protocol("about", "about", "RFC6694")
  val Acap = Protocol("acap", "application configuration access protocol", "RFC2244")
  val Acct = Protocol("acct", "acct", "RFC-ietf-appsawg-acct-uri-06")
  val Cap = Protocol("cap", "Calendar Access Protocol", "RFC4324")
  val Cid = Protocol("cid", "content identifier", "RFC2392")
  val Coap = Protocol("coap", "coap", "RFC-ietf-core-coap-18")
  val Coaps = Protocol("coaps", "coaps", "RFC-ietf-core-coap-18")
  val Crid = Protocol("crid", "TV-Anytime Content Reference Identifier", "RFC4078")
  val Data = Protocol("data", "data", "RFC2397")
  val Dav = Protocol("dav", "dav", "RFC4918")
  val Dict = Protocol("dict", "dictionary service protocol", "RFC2229")
  val Dns = Protocol("dns", "Domain Name System", "RFC4501")
  val File = Protocol("file", "Host-specific file names", "RFC1738")
  val Ftp = Protocol("ftp", "File Transfer Protocol", "RFC1738")
  val Geo = Protocol("geo", "Geographic Locations", "RFC5870")
  val Go = Protocol("go", "go", "RFC3368")
  val Gopher = Protocol("gopher", "The Gopher Protocol", "RFC4266")
  val H323 = Protocol("h323", "H.323", "RFC3508")
  val Http = Protocol("http", "Hypertext Transfer Protocol", "RFC2616")
  val Https = Protocol("https", "Hypertext Transfer Protocol Secure", "RFC2818")
  val Iax = Protocol("iax", "Inter-Asterisk eXchange Version 2", "RFC5456")
  val Icap = Protocol("icap", "Internet Content Adaptation Protocol", "RFC3507")
  val Im = Protocol("im", "Instant Messaging", "RFC3860")
  val Imap = Protocol("imap", "internet message access protocol", "RFC5092")
  val Info = Protocol("info", "Information Assets with Identifiers in Public Namespaces", "RFC4452")
  val Ipp = Protocol("ipp", "Internet Printing Protocol", "RFC3510")
  val Iris = Protocol("iris", "Internet Registry Information Service", "RFC3981")
  val IrisBeep = Protocol("iris.beep", "iris.beep", "RFC3983")
  val IrisXpc = Protocol("iris.xpc", "iris.xpc", "RFC4992")
  val IrisXpcs = Protocol("iris.xpcs", "iris.xpcs", "RFC4992")
  val IrisLwz = Protocol("iris.lwz", "iris.lwz", "RFC4993")
  val Jabber = Protocol("jabber", "jabber","Saint-Andre")
  val JarFile = Protocol("jar:file", "JAR File","JAR")
  val Ldap = Protocol("ldap", "Lightweight Directory Access Protocol","RFC4516")
  val Mailto = Protocol("mailto", "Electronic mail address", "RFC6068")
  val Mid = Protocol("mid", "message identifier", "RFC2392")
  val Msrp = Protocol("msrp", "Message Session Relay Protocol", "RFC4975")
  val Msrps = Protocol("msrps", "Message Session Relay Protocol Secure", "RFC4975")
  val Mtqp = Protocol("mtqp", "Message Tracking Query Protocol", "RFC3887")
  val Mupdate = Protocol("mupdate", "Mailbox Update (MUPDATE) Protocol", "RFC3656")
  val News = Protocol("news", "USENET news", "RFC5538")
  val Nfs = Protocol("nfs", "network file system protocol", "RFC2224")
  val Ni = Protocol("ni", "ni", "RFC6920")
  val Nih = Protocol("nih", "nih", "RFC6920")
  val Nntp = Protocol("nntp", "USENET news using NNTP access", "RFC5538")
  val Opaquelocktoken = Protocol("opaquelocktoken", "opaquelocktokent", "RFC4918")
  val Pop = Protocol("pop", "Post Office Protocol v3", "RFC2384")
  val Pres = Protocol("pres", "Presence", "RFC3859")
  val Reload = Protocol("reload", "reload","draft-ietf-p2psip-base-26")
  val Rtsp = Protocol("rtsp", "real time streaming protocol", "RFC2326")
  val Service = Protocol("service", "service location", "RFC2609")
  val Session = Protocol("session", "session", "RFC6787")
  val Shttp = Protocol("shttp", "Secure Hypertext Transfer Protocol", "RFC2660")
  val Sieve = Protocol("sieve", "ManageSieve Protocol", "RFC5804")
  val Sip = Protocol("sip", "session initiation protocol", "RFC3261")
  val Sips = Protocol("sips", "secure session initiation protocol", "RFC3261")
  val Sms = Protocol("sms", "Short Message Service", "RFC5724")
  val Snmp = Protocol("snmp", "Simple Network Management Protocol", "RFC4088")
  val SoapBeep = Protocol("soap.beep", "soap.beep", "RFC4227")
  val SoapBeeps = Protocol("soap.beeps", "soap.beeps", "RFC4227")
  val Tag = Protocol("tag", "tag", "RFC4151")
  val Tel = Protocol("tel", "telephone", "RFC3966")
  val Telnet = Protocol("telnet", "Reference to interactive sessions", "RFC4248")
  val Tftp = Protocol("tftp", "Trivial File Transfer Protocol", "RFC3617")
  val Thismessage = Protocol("thismessage", "perm/thismessage	multipart/related relative reference resolution", "RFC2557")
  val Tn3270 = Protocol("tn3270", "Interactive 3270 emulation sessions", "RFC6270")
  val Tip = Protocol("tip", "Transaction Internet Protocol", "RFC2371")
  val Tv = Protocol("tv", "TV Broadcasts", "RFC2838")
  val Urn = Protocol("urn", "Uniform Resource Names", "RFC2141][IANA registry urn-namespaces")
  val Vemmi = Protocol("vemmi", "versatile multimedia interface", "RFC2122")
  val Ws = Protocol("ws", "WebSocket connections", "RFC6455")
  val Wss = Protocol("wss", "Encrypted WebSocket connections", "RFC6455")
  val Xcon = Protocol("xcon", "xcon", "RFC6501")
  val XconUserid = Protocol("xcon-userid", "xcon-userid", "RFC6501")
  val XmlrpcBeep = Protocol("xmlrpc.beep", "xmlrpc.beep", "RFC3529")
  val XmlrpcBeeps = Protocol("xmlrpc.beeps", "xmlrpc.beeps", "RFC3529")
  val Xmpp = Protocol("xmpp", "Extensible Messaging and Presence Protocol", "RFC5122")
  val Z3950r = Protocol("z39.50r", "Z39.50 Retrieval", "RFC2056")
  val Z3950s = Protocol("z39.50s", "Z39.50 Session", "RFC2056")

  def apply(scheme: String): Protocol = schemeMap.getOrElse(scheme.toLowerCase, throw new RuntimeException(s"Unable to find $scheme in Protocol."))
}