package ca.uhn.fhir.rest.api.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.server.IRestfulServerDefaults;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.IServerOperationInterceptor;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.UrlUtil;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.json.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.net.HttpURLConnection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
// import org.json.simple.parser.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import org.hl7.fhir.instance.model.ClaimResponse;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Reference;


/*
 * #%L
 * HAPI FHIR - Server Framework
 * %%
 * Copyright (C) 2014 - 2018 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

public abstract class RequestDetails {

	private String myTenantId;
	private String myCompartmentName;
	private String myCompleteUrl;
	private String myFhirServerBase;
	private IIdType myId;
	private String myOperation;
	private Map<String, String[]> myParameters;
	private byte[] myRequestContents;
	private IRequestOperationCallback myRequestOperationCallback = new RequestOperationCallback();
	private String myRequestPath;
	private RequestTypeEnum myRequestType;
	private String myResourceName;
	private boolean myRespondGzip;
	private IRestfulResponse myResponse;
	private RestOperationTypeEnum myRestOperationType;
	private String mySecondaryOperation;
	private boolean mySubRequest;
	private Map<String, List<String>> myUnqualifiedToQualifiedNames;
	private Map<Object, Object> myUserData;

	public void addParameter(String theName, String[] theValues) {
		getParameters();
		myParameters.put(theName, theValues);
	}

	protected abstract byte[] getByteStreamRequestContents();

	/**
	 * Return the charset as defined by the header contenttype. Return null if it is not set.
	 */
	public abstract Charset getCharset();

	public String getCompartmentName() {
		return myCompartmentName;
	}

	public void setCompartmentName(String theCompartmentName) {
		myCompartmentName = theCompartmentName;
	}

	public String getCompleteUrl() {
		return myCompleteUrl;
	}

	public void setCompleteUrl(String theCompleteUrl) {
		myCompleteUrl = theCompleteUrl;
	}

	/**
	 * Returns the <b>conditional URL</b> if this request has one, or <code>null</code> otherwise. For an
	 * update or delete method, this is the part of the URL after the <code>?</code>. For a create, this
	 * is the value of the <code>If-None-Exist</code> header.
	 *
	 * @param theOperationType The operation type to find the conditional URL for
	 * @return Returns the <b>conditional URL</b> if this request has one, or <code>null</code> otherwise
	 */
	public String getConditionalUrl(RestOperationTypeEnum theOperationType) {
		if (theOperationType == RestOperationTypeEnum.CREATE) {
			String retVal = this.getHeader(Constants.HEADER_IF_NONE_EXIST);
			if (isBlank(retVal)) {
				return null;
			}
			if (retVal.startsWith(this.getFhirServerBase())) {
				retVal = retVal.substring(this.getFhirServerBase().length());
			}
			return retVal;
		} else if (theOperationType != RestOperationTypeEnum.DELETE && theOperationType != RestOperationTypeEnum.UPDATE) {
			return null;
		}

		if (this.getId() != null && this.getId().hasIdPart()) {
			return null;
		}

		int questionMarkIndex = this.getCompleteUrl().indexOf('?');
		if (questionMarkIndex == -1) {
			return null;
		}

		return this.getResourceName() + this.getCompleteUrl().substring(questionMarkIndex);
	}

	/**
	 * Returns the HAPI FHIR Context associated with this request
	 */
	public abstract FhirContext getFhirContext();

	/**
	 * The fhir server base url, independant of the query being executed
	 *
	 * @return the fhir server base url
	 */
	public String getFhirServerBase() {
		return myFhirServerBase;
	}

	public void setFhirServerBase(String theFhirServerBase) {
		myFhirServerBase = theFhirServerBase;
	}

	public abstract String getHeader(String name);

	public abstract List<String> getHeaders(String name);

	public IIdType getId() {
		return myId;
	}

	public void setId(IIdType theId) {
		myId = theId;
	}

	/**
	 * Retrieves the body of the request as binary data. Either this method or {@link #getReader} may be called to read
	 * the body, not both.
	 *
	 * @return a {@link InputStream} object containing the body of the request
	 * @throws IllegalStateException if the {@link #getReader} method has already been called for this request
	 * @throws IOException           if an input or output exception occurred
	 */
	public abstract InputStream getInputStream() throws IOException;

	public String getOperation() {
		return myOperation;
	}

	public void setOperation(String theOperation) {
		myOperation = theOperation;
	}

	public Map<String, String[]> getParameters() {
		if (myParameters == null) {
			myParameters = new HashMap<>();
		}
		return Collections.unmodifiableMap(myParameters);
	}

	public void setParameters(Map<String, String[]> theParams) {
		myParameters = theParams;
		myUnqualifiedToQualifiedNames = null;

		// Sanitize keys if necessary to prevent injection attacks
		boolean needsSanitization = false;
		for (String nextKey : theParams.keySet()) {
			if (UrlUtil.isNeedsSanitization(nextKey)) {
				needsSanitization = true;
				break;
			}
		}
		if (needsSanitization) {
			myParameters = myParameters
				.entrySet()
				.stream()
				.collect(Collectors.toMap(t -> UrlUtil.sanitizeUrlPart((String) ((Map.Entry) t).getKey()), t -> (String[]) ((Map.Entry) t).getValue()));
		}
	}

	/**
	 * Retrieves the body of the request as character data using a <code>BufferedReader</code>. The reader translates the
	 * character data according to the character encoding used on the body. Either this method or {@link #getInputStream}
	 * may be called to read the body, not both.
	 *
	 * @return a <code>Reader</code> containing the body of the request
	 * @throws UnsupportedEncodingException if the character set encoding used is not supported and the text cannot be decoded
	 * @throws IllegalStateException        if {@link #getInputStream} method has been called on this request
	 * @throws IOException                  if an input or output exception occurred
	 * @see javax.servlet.http.HttpServletRequest#getInputStream
	 */
	public abstract Reader getReader() throws IOException;

	/**
	 * Returns an invoker that can be called from user code to advise the server interceptors
	 * of any nested operations being invoked within operations. This invoker acts as a proxy for
	 * all interceptors
	 */
	public IRequestOperationCallback getRequestOperationCallback() {
		return myRequestOperationCallback;
	}

	/**
	 * The part of the request URL that comes after the server base.
	 * <p>
	 * Will not contain a leading '/'
	 * </p>
	 */
	public String getRequestPath() {
		return myRequestPath;
	}

	public void setRequestPath(String theRequestPath) {
		assert theRequestPath.length() == 0 || theRequestPath.charAt(0) != '/';
		myRequestPath = theRequestPath;
	}

	public RequestTypeEnum getRequestType() {
		return myRequestType;
	}

	public void setRequestType(RequestTypeEnum theRequestType) {
		myRequestType = theRequestType;
	}

	public String getResourceName() {
		return myResourceName;
	}

	public void setResourceName(String theResourceName) {
		myResourceName = theResourceName;
	}

	public IRestfulResponse getResponse() {
		return myResponse;
	}

	public void setResponse(IRestfulResponse theResponse) {
		this.myResponse = theResponse;
	}

	public RestOperationTypeEnum getRestOperationType() {
		return myRestOperationType;
	}

	public void setRestOperationType(RestOperationTypeEnum theRestOperationType) {
		myRestOperationType = theRestOperationType;
	}

	public String getSecondaryOperation() {
		return mySecondaryOperation;
	}

	public void setSecondaryOperation(String theSecondaryOperation) {
		mySecondaryOperation = theSecondaryOperation;
	}

	public abstract IRestfulServerDefaults getServer();

	/**
	 * Returns the server base URL (with no trailing '/') for a given request
	 */
	public abstract String getServerBaseForRequest();

	public String getTenantId() {
		return myTenantId;
	}

	public void setTenantId(String theTenantId) {
		myTenantId = theTenantId;
	}

	public Map<String, List<String>> getUnqualifiedToQualifiedNames() {
		if (myUnqualifiedToQualifiedNames == null) {
			for (String next : myParameters.keySet()) {
				for (int i = 0; i < next.length(); i++) {
					char nextChar = next.charAt(i);
					if (nextChar == ':' || nextChar == '.') {
						if (myUnqualifiedToQualifiedNames == null) {
							myUnqualifiedToQualifiedNames = new HashMap<>();
						}
						String unqualified = next.substring(0, i);
						List<String> list = myUnqualifiedToQualifiedNames.get(unqualified);
						if (list == null) {
							list = new ArrayList<>(4);
							myUnqualifiedToQualifiedNames.put(unqualified, list);
						}
						list.add(next);
						break;
					}
				}
			}
		}

		if (myUnqualifiedToQualifiedNames == null) {
			myUnqualifiedToQualifiedNames = Collections.emptyMap();
		}

		return myUnqualifiedToQualifiedNames;
	}

	/**
	 * Returns a map which can be used to hold any user specific data to pass it from one
	 * part of the request handling chain to another. Data in this map can use any key, although
	 * user code should try to use keys which are specific enough to avoid conflicts.
	 * <p>
	 * A new map is created for each individual request that is handled by the server,
	 * so this map can be used (for example) to pass authorization details from an interceptor
	 * to the resource providers, or from an interceptor's {@link IServerInterceptor#incomingRequestPreHandled(RestOperationTypeEnum, ca.uhn.fhir.rest.server.interceptor.IServerInterceptor.ActionRequestDetails)}
	 * method to the {@link IServerInterceptor#outgoingResponse(RequestDetails, org.hl7.fhir.instance.model.api.IBaseResource, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
	 * method.
	 * </p>
	 */
	public Map<Object, Object> getUserData() {
		if (myUserData == null) {
			myUserData = new HashMap<>();
		}
		return myUserData;
	}

	public boolean isRespondGzip() {
		return myRespondGzip;
	}

	public void setRespondGzip(boolean theRespondGzip) {
		myRespondGzip = theRespondGzip;
	}

	/**
	 * Is this request a sub-request (i.e. a request within a batch or transaction)? This
	 * flag is used internally by hapi-fhir-jpaserver-base, but not used in the plain server
	 * library. You may use it in your client code as a hint when implementing transaction logic in the plain
	 * server.
	 * <p>
	 * Defaults to {@literal false}
	 * </p>
	 */
	public boolean isSubRequest() {
		return mySubRequest;
	}

	/**
	 * Is this request a sub-request (i.e. a request within a batch or transaction)? This
	 * flag is used internally by hapi-fhir-jpaserver-base, but not used in the plain server
	 * library. You may use it in your client code as a hint when implementing transaction logic in the plain
	 * server.
	 * <p>
	 * Defaults to {@literal false}
	 * </p>
	 */
	public void setSubRequest(boolean theSubRequest) {
		mySubRequest = theSubRequest;
	}

	public final byte[] loadRequestContents() {
		if (myRequestContents == null) {
			myRequestContents = getByteStreamRequestContents();
		}
		return myRequestContents;
	}

	public void removeParameter(String theName) {
		Validate.notNull(theName, "theName must not be null");
		getParameters();
		myParameters.remove(theName);
	}

	/**
	 * This method may be used to modify the contents of the incoming
	 * request by hardcoding a value which will be used instead of the
	 * value received by the client.
	 * <p>
	 * This method is useful for modifying the request body prior
	 * to parsing within interceptors. It generally only has an
	 * impact when called in the {@link IServerInterceptor#incomingRequestPostProcessed(RequestDetails, HttpServletRequest, HttpServletResponse)}
	 * method
	 * </p>
	 */
	public void setRequestContents(byte[] theRequestContents) {
		myRequestContents = theRequestContents;
	}

	private class RequestOperationCallback implements IRequestOperationCallback {

		private List<IServerInterceptor> getInterceptors() {
			if (getServer() == null) {
				return Collections.emptyList();
			}
			return getServer().getInterceptors();
		}
		
		
		public void submitDocs(IBaseResource i) {
			System.out.println("in submit docss method");
			Communication com = (Communication)i;
			System.out.println(com.getId());
			List<Reference> refs = com.getAbout();
			for (Iterator iterator = refs.iterator(); iterator.hasNext();) {
				Reference reference = (Reference) iterator.next();
				IBaseResource bas = reference.getResource();
				if  (bas instanceof Claim) {
					Claim new_name = (Claim) bas;
					System.out.println(new_name.getId());
				}

			}
		}

		@Override
		public void resourceCreated(IBaseResource theResource) {
			System.out.println("-----------Resource Created -------");
			System.out.println(theResource.getClass().getMethods());
			this.submitDocs(theResource);
//			ClaimResponse cr =  new ClaimResponse();
			FhirContext fhirContext = FhirContext.forR4();
			final IParser iParser = fhirContext.newJsonParser();
			String resourceJson = iParser.encodeResourceToString(theResource);
			
//			Gson g = new Gson();
//			System.out.println("ResOURCE JSON :"+resourceJson);
			
			try {
//				String requestBody = new String(RequestDetails.this.loadRequestContents(), Constants.CHARSET_UTF8);
//				System.out.println(requestBody);
				JSONObject resourceJsonObj = new JSONObject(resourceJson);
				JSONObject newResource = new JSONObject();
				newResource.put("resourceType","Patient");
				newResource.put("req_contents",resourceJsonObj);
				newResource.put("id",theResource.getIdElement().getIdPart());
				System.out.println("Body Resource  : "+newResource);
				
				CloseableHttpClient client = HttpClients.createDefault();
				HttpPost httpPost = new HttpPost("http://localhost:8090/fhir/Patient");
				StringEntity entity = new StringEntity(newResource.toString());
				httpPost.setEntity(entity);
				httpPost.setHeader("Accept", "application/json");
				httpPost.setHeader("Content-type", "application/json");
				CloseableHttpResponse response = client.execute(httpPost);
				client.close();
			}
			catch(JSONException json_ex) {
		    	 json_ex.printStackTrace();
		      } 
			catch(Exception ex) {
				ex.printStackTrace();
			}
			//		    String json = "{"id":1,"name":"John"}";
//		    String json = "{"id":1,"name":"John"}";
		/*
            StringEntity entity = new StringEntity(requestBody);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = client.execute(httpPos$
            client.close();
            */
//		    HttpPost httpPost = new HttpPost("http://localhost:8090/fhir/Patient/2");
		    
//			params = UrlUtil.parseQueryStrings(theRequest.getQueryString(), requestBody);
		      /*
		      String clientId = "app-login";
		      CloseableHttpClient client = HttpClients.createDefault();
		      HttpPost httpPost = new HttpPost("https://54.227.173.76:8443/auth/realms/ClientFhirServer/protocol/openid-connect/token");
		      List<NameValuePair> params = new ArrayList<NameValuePair>();
		      params.add(new BasicNameValuePair("client_id", "app-login"));
		      params.add(new BasicNameValuePair("username","john"));
		      params.add(new BasicNameValuePair("password", "john123"));
		      params.add(new BasicNameValuePair("grant_type", "password"));
		      try {
		        httpPost.setEntity(new UrlEncodedFormEntity(params));
		      } catch (UnsupportedEncodingException e) {
		        e.printStackTrace();
		      }
//		      System.out.println("introspectUrl::");
//		      System.out.println(introspectUrl);
		      JsonObject tokenResponse;
		      
		      
		      // Map<String,Object> params = new LinkedHashMap<>();
		      try {
		        CloseableHttpResponse response = client.execute(httpPost);
		        String jsonString = EntityUtils.toString(response.getEntity());
		        
		        tokenResponse = new JsonParser().parse(jsonString).getAsJsonObject();
		        client.close();
		      }
		      catch (IOException e) {
//		        System.out.println("\n\n\\n\n\n\\n\n\n\n\nEXceptionnnnnn");
		        e.printStackTrace();
		        tokenResponse = null;
		      }
		      System.out.println("===tokenResponse==");
		      System.out.println(tokenResponse);
		      JSONObject reqJson = new JSONObject();
		      StringBuilder sb = new StringBuilder();
		      try {
			      URL url = new URL("http://localhost:8080/payer_fhir/baseDstu3/ClaimResponse");
			        Gson gsonObj = new Gson();
			        String json_string = "{\n" + 
			        		"  \"resourceType\": \"Claim\",\n" + 
			        		"  \"id\": \"100150\",\n" + 
			        		"  \"text\": {\n" + 
			        		"    \"status\": \"generated\",\n" + 
			        		"    \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">A human-readable rendering of the Oral Health Claim</div>\"\n" + 
			        		"  },\n" + 
			        		"  \"identifier\": [\n" + 
			        		"    {\n" + 
			        		"      \"system\": \"http://happyvalley.com/claim\",\n" + 
			        		"      \"value\": \"12345\"\n" + 
			        		"    }\n" + 
			        		"  ],\n" + 
			        		"  \"status\": \"active\",\n" + 
			        		"  \"type\": {\n" + 
			        		"    \"coding\": [\n" + 
			        		"      {\n" + 
			        		"        \"system\": \"http://terminology.hl7.org/CodeSystem/claim-type\",\n" + 
			        		"        \"code\": \"oral\"\n" + 
			        		"      }\n" + 
			        		"    ]\n" + 
			        		"  },\n" + 
			        		"  \"use\": {\"code\":\"claim\"},\n" + 
			        		"  \"patient\": {\n" + 
			        		"    \"reference\": \"Patient/1\"\n" + 
			        		"  },\n" + 
			        		"  \"created\": \"2014-08-16\",\n" + 
			        		"\n" + 
			        		"  \"priority\": {\n" + 
			        		"    \"coding\": [\n" + 
			        		"      {\n" + 
			        		"        \"code\": \"normal\"\n" + 
			        		"      }\n" + 
			        		"    ]\n" + 
			        		"  },\n" + 
			        		"  \"payee\": {\n" + 
			        		"    \"type\": {\n" + 
			        		"      \"coding\": [\n" + 
			        		"        {\n" + 
			        		"          \"code\": \"provider\"\n" + 
			        		"        }\n" + 
			        		"      ]\n" + 
			        		"    }\n" + 
			        		"  },\n" + 
			        		"  \n" + 
			        		"  \"diagnosis\": [\n" + 
			        		"    {\n" + 
			        		"      \"sequence\": 1,\n" + 
			        		"      \"diagnosisCodeableConcept\": {\n" + 
			        		"        \"coding\": [\n" + 
			        		"          {\n" + 
			        		"            \"code\": \"123456\"\n" + 
			        		"          }\n" + 
			        		"        ]\n" + 
			        		"      }\n" + 
			        		"    }\n" + 
			        		"  ],\n" + 
			        		"  \"insurance\": [\n" + 
			        		"    {\n" + 
			        		"      \"sequence\": 1,\n" + 
			        		"      \"focal\": true,\n" + 
			        		"      \"identifier\": {\n" + 
			        		"        \"system\": \"http://happyvalley.com/claim\",\n" + 
			        		"        \"value\": \"12345\"\n" + 
			        		"      }\n" + 
			        		"      \n" + 
			        		"    }\n" + 
			        		"  ],\n" + 
			        		"  \"item\": [\n" + 
			        		"    {\n" + 
			        		"      \"sequence\": 1,\n" + 
			        		"      \"careTeamSequence\": [\n" + 
			        		"        1\n" + 
			        		"      ],\n" + 
			        		"      \"productOrService\": {\n" + 
			        		"        \"coding\": [\n" + 
			        		"          {\n" + 
			        		"            \"code\": \"1200\"\n" + 
			        		"          }\n" + 
			        		"        ]\n" + 
			        		"      },\n" + 
			        		"      \"servicedDate\": \"2014-08-16\",\n" + 
			        		"      \"unitPrice\": {\n" + 
			        		"        \"value\": 135.57,\n" + 
			        		"        \"currency\": \"USD\"\n" + 
			        		"      },\n" + 
			        		"      \"net\": {\n" + 
			        		"        \"value\": 135.57,\n" + 
			        		"        \"currency\": \"USD\"\n" + 
			        		"      }\n" + 
			        		"    }\n" + 
			        		"  ]\n" + 
			        		"}";
			        
//			        reqJson.put("request_for", "requirements");
			        reqJson = new JSONObject(json_string);
			        String jsonStr = reqJson.toString();
			        System.out.println(jsonStr);
			        byte[] postDataBytes = jsonStr.getBytes("UTF-8");
			
			        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			        conn.setRequestMethod("POST");
			        conn.setRequestProperty("Content-Type", "application/json");
			        conn.setRequestProperty("Accept","application/json");
			        conn.setRequestProperty("Authorization","Bearer "+tokenResponse.get("access_token").toString());
			        conn.setDoOutput(true);
			        conn.getOutputStream().write(postDataBytes);
			        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			        String line =null;
			        while((line=in.readLine())!= null){
			          sb.append(line);
			        }
				*/
					for (IServerInterceptor next : getInterceptors()) {
						if (next instanceof IServerOperationInterceptor) {
							((IServerOperationInterceptor) next).resourceCreated(RequestDetails.this, theResource);
						}
					}
				/*	
					System.out.println("----------fhirResponse");
					System.out.println(sb.toString());
		      }
		      catch (Exception exception) {
			        System.out.println("\n\n\\n\n\n\\n\n\n\n\nEXceptionnnnnn");
			        exception.printStackTrace();
			    }
			    */
			   
			
			
			
		}

		@Override
		public void resourceDeleted(IBaseResource theResource) {
			for (IServerInterceptor next : getInterceptors()) {
				if (next instanceof IServerOperationInterceptor) {
					((IServerOperationInterceptor) next).resourceDeleted(RequestDetails.this, theResource);
				}
			}
		}

		@Override
		public void resourcePreCreate(IBaseResource theResource) {
			for (IServerInterceptor next : getInterceptors()) {
				if (next instanceof IServerOperationInterceptor) {
					((IServerOperationInterceptor) next).resourcePreCreate(RequestDetails.this, theResource);
				}
			}
		}

		@Override
		public void resourcePreDelete(IBaseResource theResource) {
			for (IServerInterceptor next : getInterceptors()) {
				if (next instanceof IServerOperationInterceptor) {
					((IServerOperationInterceptor) next).resourcePreDelete(RequestDetails.this, theResource);
				}
			}
		}

		@Override
		public void resourcePreUpdate(IBaseResource theOldResource, IBaseResource theNewResource) {
			for (IServerInterceptor next : getInterceptors()) {
				if (next instanceof IServerOperationInterceptor) {
					((IServerOperationInterceptor) next).resourcePreUpdate(RequestDetails.this, theOldResource, theNewResource);
				}
			}
		}

		/**
		 * @deprecated Deprecated in HAPI FHIR 2.6 - Use {@link IRequestOperationCallback#resourceUpdated(IBaseResource, IBaseResource)} instead
		 */
		@Deprecated
		@Override
		public void resourceUpdated(IBaseResource theResource) {
			for (IServerInterceptor next : getInterceptors()) {
				if (next instanceof IServerOperationInterceptor) {
					((IServerOperationInterceptor) next).resourceUpdated(RequestDetails.this, theResource);
				}
			}
		}

		@Override
		public void resourceUpdated(IBaseResource theOldResource, IBaseResource theNewResource) {
			for (IServerInterceptor next : getInterceptors()) {
				if (next instanceof IServerOperationInterceptor) {
					((IServerOperationInterceptor) next).resourceUpdated(RequestDetails.this, theOldResource, theNewResource);
				}
			}
		}

		@Override
		public void resourcesCreated(Collection<? extends IBaseResource> theResource) {
			for (IBaseResource next : theResource) {
				resourceCreated(next);
			}
		}

		@Override
		public void resourcesDeleted(Collection<? extends IBaseResource> theResource) {
			for (IBaseResource next : theResource) {
				resourceDeleted(next);
			}
		}

		/**
		 * @deprecated Deprecated in HAPI FHIR 2.6 - Use {@link IRequestOperationCallback#resourceUpdated(IBaseResource, IBaseResource)} instead
		 */
		@Deprecated
		public void resourcesUpdated(Collection<? extends IBaseResource> theResource) {
			for (IBaseResource next : theResource) {
				resourceUpdated(next);
			}
		}

	}

}
