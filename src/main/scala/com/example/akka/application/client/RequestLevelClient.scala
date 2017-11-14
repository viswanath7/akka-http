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
	
	/**
		* Akka HTTP is streaming all the way through, which means that back-pressure mechanisms enabled by Akka Streams are
		* exposed through all layers–from TCP layer, through HTTP server to user-facing HttpRequest and HttpResponse
		* and their HttpEntity APIs. It means, lack of consumption of the HTTP Entity, is signaled as back-pressure to
		* the other side of the connection.
		*
		* Consuming (or discarding) the Entity of a request is mandatory! If accidentally left neither consumed or discarded
		* Akka HTTP will assume the incoming data should remain back-pressured, and will stall the incoming data via
		* TCP back-pressure mechanisms. A client should consume the Entity regardless of the status of the HttpResponse.
		*/
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
		case statusCode =>
			logger error s"Failed to retrieve ip address. HTTP response status code : $statusCode"
			/**
				* We do not care about the response payload but the entity still has to be consumed otherwise,
				* one would exert back-pressure on underlying TCP client.
				* The following method pipes incoming bytes directly to Sink.ignore
				*/
			httpResponse.discardEntityBytes.future.onComplete{ _ => logger debug "HTTP response entity discarded!"}
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
