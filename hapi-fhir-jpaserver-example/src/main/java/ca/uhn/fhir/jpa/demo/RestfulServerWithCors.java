
package ca.uhn.fhir.jpa.demo;

import java.util.Collection;
import java.util.List;

import ca.uhn.fhir.jpa.provider.dstu3.TerminologyUploaderProviderDstu3;
import ca.uhn.fhir.jpa.subscription.email.SubscriptionEmailInterceptor;
import ca.uhn.fhir.jpa.subscription.resthook.SubscriptionRestHookInterceptor;
import ca.uhn.fhir.jpa.subscription.websocket.SubscriptionWebsocketInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu2;
import ca.uhn.fhir.jpa.provider.JpaSystemProviderDstu2;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.model.dstu2.composite.MetaDt;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.*;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;

import javax.servlet.http.*;
import javax.servlet.*;  
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import java.util.Arrays;

import javax.servlet.annotation.WebServlet;

import org.hl7.fhir.instance.hapi.validation.FhirInstanceValidator;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.interceptor.*;
import ca.uhn.fhir.validation.ResultSeverityEnum;



@WebServlet(urlPatterns = { "/fhir/*" }, displayName = "FHIR Server")
public class RestfulServerWithCors extends RestfulServer {
 
   @Override
   protected void initialize() throws ServletException {
       
//      // ... define your resource providers here ...
// 
//      // Define your CORS configuration. This is an example
//      // showing a typical setup. You should customize this
//      // to your specific needs 
//      CorsConfiguration config = new CorsConfiguration();
//      config.addAllowedHeader("x-fhir-starter");
//      config.addAllowedHeader("Origin");
//      config.addAllowedHeader("Accept");
//      config.addAllowedHeader("X-Requested-With");
//      config.addAllowedHeader("Content-Type");
// 
//      config.addAllowedOrigin("*");
// 
//      config.addExposedHeader("Location");
//      config.addExposedHeader("Content-Location");
//      config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
// 
//      // Create the interceptor and register it
//      CorsInterceptor interceptor = new CorsInterceptor(config);
//      registerInterceptor(interceptor);
 
   }
   
   
//	private static final long serialVersionUID = 1L;
//
//	private WebApplicationContext myAppCtx;
//
//
//	/////////////////////////////////////////////////////
//	/////////////////////////////////////////////////////RemovableSAMPLE
//	/////////////////////////////////////////////////////
//	/////////////////////////////////////////////////////
//	@Override
//	public void doGet(HttpServletRequest request, HttpServletResponse response){  
//       try {
//	        HttpSession session=request.getSession();  
//	        System.out.println("\n\n\n\n\n\n\n\n session.getAttributeNames()");
//	        System.out.println(session.getAttributeNames());
//	        System.out.println(session.getAttribute("uname"));
//	        session.setAttribute("uname","temp");  
//	        System.out.println("session.getAttribute unbmae");
//	        System.out.println(session.getAttribute("uname"));
//       	handleRequest(RequestTypeEnum.GET, request, response);
//       } catch (Exception e) {
//       	 System.out.println("Exception");
//       }
//        
//   }
    
}