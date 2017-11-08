package com.example.akka.application.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import org.slf4j.LoggerFactory

import scala.language.postfixOps
import scala.util.{Failure, Success}

object RequestLevelClient extends App with JsonSupport {
	
	val logger = LoggerFactory getLogger RequestLevelClient.getClass
	
	implicit val actorSystem = ActorSystem("request-level-client-side-api-actor-system")
	implicit val actorMaterialiser = ActorMaterializer(ActorMaterializerSettings(actorSystem)
		.withInputBuffer(initialSize = 16, maxSize = 8192))
	implicit val executionContext = actorSystem.dispatcher
	
	val ipAddressAPIURL = "https://api.ipify.org?format=json"
	
	Http()
		.singleRequest(HttpRequest(uri = ipAddressAPIURL))
		.onComplete {
			case Success(httpResponse) =>
				extractIPAddress(httpResponse)
				shutdown()
			case Failure(error) =>
				logger error("Error encountered while determining IP address", error)
				shutdown()
		}
	
	private def extractIPAddress(httpResponse: HttpResponse) = httpResponse.status match {
		case StatusCodes.OK => Unmarshal(httpResponse.entity).to[InternetProtocolAddress].onComplete {
			case Success(internetProtocolAddress) => logger info s"Your IP address is ${internetProtocolAddress.ip}"
			case Failure(error) => logger error("Error encountered while unmarshalling response entity", error)
		}
		case statusCode => logger error s"Failed to retrieve ip address. HTTP response status code : $statusCode"
	}
	
	
	private def shutdown(): Unit = {
		Http().shutdownAllConnectionPools().onComplete { _ =>
			logger info s"Terminating actor system '${actorSystem.name}'"
			actorSystem.terminate().andThen {
				case Success(t) => logger info "Terminated!"
				case Failure(_) => logger error "Failed to terminate the actor system!"
			}
		}
	}
	
}
