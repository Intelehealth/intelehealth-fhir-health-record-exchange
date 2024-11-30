package org.ih.health.record.exchange.config;


import org.ih.health.record.exchange.utils.IHConstant;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;

@Component
public class FhirConfig extends IHConstant{
	FhirContext fhirContext = FhirContext.forR4();
	
	public  IGenericClient getOpenCRFhirContext(){	
		System.err.println("opencrOpenhimURL:"+opencrOpenhimURL);
		IGenericClient openCr = fhirContext.newRestfulGenericClient(opencrOpenhimURL);
		BasicAuthInterceptor b= new BasicAuthInterceptor(opencrOpenhimAuthentication);		
		openCr.registerInterceptor(b);
		return openCr;		
		
	}
	
	public  IGenericClient getLocalOpenMRSFhirContext(){		
		IGenericClient openMRSServer = fhirContext.newRestfulGenericClient(localOpenmrsOpenhimURL+"/ws/fhir2/R4");
		BasicAuthInterceptor openmrsAuthentication = new BasicAuthInterceptor(localOpenmrsOpenhimAuthentication);
		openMRSServer.registerInterceptor(openmrsAuthentication);
		return openMRSServer;		
		
	}
	
	public  IGenericClient getGOFRFhirContext(){		
		IGenericClient openMRSServer = fhirContext.newRestfulGenericClient(gofrOpenhimURL);
		BasicAuthInterceptor openmrsAuthentication = new BasicAuthInterceptor(gofrOpenhimAuthentication);
		openMRSServer.registerInterceptor(openmrsAuthentication);
		return openMRSServer;		
		
	}
	
	public String[] getOpenMRSCredentials(){
		 return localOpenmrsOpenhimAuthentication.split(":");
		
	}



}
