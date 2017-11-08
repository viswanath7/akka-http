package com.example.akka.application

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

package object client {
	
	// Domain model
	case class InternetProtocolAddress(ip: String)
	
	/**
		* Collects JSON format instances so that the support trait can be used
		* whenever JSON marshalling or unmarshalling is required
		*/
	trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
		implicit val ipAddressFormat = jsonFormat1(InternetProtocolAddress)
	}
	
}
