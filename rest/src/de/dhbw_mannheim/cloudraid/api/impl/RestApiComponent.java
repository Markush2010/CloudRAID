/*
 * Copyright 2011 - 2012 by the CloudRAID Team
 * see AUTHORS for more details
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.dhbw_mannheim.cloudraid.api.impl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import de.dhbw_mannheim.cloudraid.api.responses.IRestApiResponse;
import de.dhbw_mannheim.cloudraid.config.ICloudRAIDConfig;
import de.dhbw_mannheim.cloudraid.core.ICloudRAIDService;
import de.dhbw_mannheim.cloudraid.metadatamgr.IMetadataManager;

/**
 * @author Markus Holtermann
 * 
 */
public class RestApiComponent {

	/**
	 * Main path for the REST API
	 */
	private static final String SERVLET_ALIAS = "/";

	/**
	 * @param req
	 * @param resp
	 */
	public static void error404(HttpServletRequest req, IRestApiResponse resp) {
		resp.setStatusCode(404);
		resp.writeLine("No page matching " + req.getPathInfo());
	}

	/**
	 * @param req
	 * @param resp
	 * @param msg
	 */
	public static void error500(HttpServletRequest req, IRestApiResponse resp,
			String msg) {
		resp.setStatusCode(500);
		resp.writeLine("Servererror for page " + req.getPathInfo());
		resp.writeField("msg", msg);
	}

	/**
	 * Service that handles all the request. Injected by the component.xml
	 */
	private HttpService httpService = null;

	/**
	 * @param httpService
	 */
	protected synchronized void setHttpService(HttpService httpService) {
		System.out.println("RestApiComponent: setHttpService: begin");
		this.httpService = httpService;
		System.out.println("RestApiComponent: setHttpService: "
				+ this.httpService);
		System.out.println("RestApiComponent: setHttpService: end");
	}

	/**
	 * Unregister the service.
	 */
	protected void shutdown() {
		System.out.println("RestApiComponent: shutdown: begin");
		this.httpService.unregister(RestApiComponent.SERVLET_ALIAS);
		System.out.println("RestApiComponent: shutdown: end");
	}

	/**
	 * Initialize and start the service.
	 * 
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 */
	protected void startup() throws IllegalArgumentException,
			SecurityException, NoSuchMethodException, InstantiationException {
		System.out.println("RestApiComponent: startup: begin");
		try {
			System.out.println("Staring up sevlet at "
					+ RestApiComponent.SERVLET_ALIAS);
			RestApiServlet servlet = new RestApiServlet();
			this.httpService.registerServlet(RestApiComponent.SERVLET_ALIAS,
					servlet, null, null);
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (NamespaceException e) {
			e.printStackTrace();
		}
		System.out.println("RestApiComponent: startup: end");
	}

	protected synchronized void unsetCloudRAIDService(ICloudRAIDService core) {
		System.out.println("RestApiComponent: unsetCloudRAIDService: begin");
		System.out.println("RestApiComponent: unsetCloudRAIDService: " + core);
		this.httpService.unregister(RestApiComponent.SERVLET_ALIAS);
		System.out.println("RestApiComponent: unsetCloudRAIDService: end");
	}

	/**
	 * @param config
	 */
	protected synchronized void unsetConfig(ICloudRAIDConfig config) {
		System.out.println("RestApiComponent: unsetConfig: begin");
		System.out.println("RestApiComponent: unsetConfig: " + config);
		this.httpService.unregister(RestApiComponent.SERVLET_ALIAS);
		System.out.println("RestApiComponent: unsetConfig: end");
	}

	/**
	 * @param httpService
	 */
	protected synchronized void unsetHttpService(HttpService httpService) {
		System.out.println("RestApiComponent: unsetHttpService: begin");
		System.out
				.println("RestApiComponent: unsetHttpService: " + httpService);
		httpService.unregister(RestApiComponent.SERVLET_ALIAS);
		this.httpService = null;
		System.out.println("RestApiComponent: unsetHttpService: "
				+ this.httpService);
		System.out.println("RestApiComponent: unsetHttpService: end");
	}

	/**
	 * @param metadataService
	 */
	protected synchronized void unsetMetadata(IMetadataManager metadataService) {
		System.out.println("RestApiComponent: unsetMetadataMgr: begin");
		System.out.println("RestApiComponent: unsetMetadataMgr: "
				+ metadataService);
		this.httpService.unregister(RestApiComponent.SERVLET_ALIAS);
		System.out.println("RestApiComponent: unsetMetadataMgr: end");
	}

}