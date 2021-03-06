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

package de.dhbw_mannheim.cloudraid.amazons3.impl.net.oauth;

import org.scribe.builder.api.Api;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

/**
 * @author Markus Holtermann
 * 
 */
public class AmazonS3Service implements OAuthService {

	/**
	 * The OAuth version, even if it is not real OAuth
	 */
	private static final String VERSION = "1.0";

	/**
	 * The {@link Api} for this {@link OAuthService}
	 */
	private AmazonS3Api api;

	/**
	 * The config that is used for this {@link OAuthService}
	 */
	private OAuthConfig config;

	/**
	 * This constructs a new OAuthService for AmazonS3 that handles the
	 * non-standard login and access-token generation
	 * 
	 * @param api
	 *            OAuth 1.0a api information
	 * @param config
	 *            OAuth 1.0a configuration param object
	 */
	public AmazonS3Service(AmazonS3Api api, OAuthConfig config) {
		this.api = api;
		this.config = config;
	}

	/**
	 * Not needed for Amazon S3!
	 */
	@Override
	public Token getAccessToken(Token requestToken, Verifier verifier) {
		return null;
	}

	/**
	 * Not needed for Amazon S3!
	 */
	@Override
	public String getAuthorizationUrl(Token requestToken) {
		return "";
	}

	/**
	 * This function builds the bucket endpoint. If the bucket contains upper
	 * case characters an URL styled endpoint (
	 * <code>https://s3.amazonaws.com/ThiIsABucket/</code>) is returned.
	 * Otherwise the return value is in domain style and similar to
	 * <code>https://thisisabucket.s3.amazonaws.com/</code>.
	 * 
	 * @param bucket
	 *            The requested bucket
	 * @return The bucket endpoint with taking care of capital letters
	 */
	public String getBucketEndpoint(String bucket) {
		return this.api.getS3Endpoint() + bucket + "/";
	}

	/**
	 * Not needed for Amazon S3!
	 */
	@Override
	public Token getRequestToken() {
		return null;
	}

	/**
	 * 
	 * @return Returns the Amazon S3 base URL:
	 *         <code>https://s3.amazonaws.com</code>
	 */
	public String getS3Endpoint() {
		return this.api.getS3Endpoint();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getVersion() {
		return AmazonS3Service.VERSION;
	}

	/**
	 * Signs the given {@link OAuthRequest}
	 * 
	 * @param request
	 *            The request to sign
	 */
	public void signRequest(OAuthRequest request) {
		System.err.println("[DEBUG] AmazonS3Service.signRequest(): request = "
				+ request);

		String baseString = this.api.getHeaderExtractor().extract(request);

		String signature = this.api.getSignatureService().getSignature(
				baseString, this.config.getApiSecret());

		String signHeader = "AWS" + " " + this.config.getApiKey() + ":"
				+ signature;
		request.addHeader(OAuthConstants.HEADER, signHeader);
		System.err
				.println("[DEBUG] AmazonS3Service.addSignature(): Authorization = "
						+ request.getHeaders().get("Authorization"));

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void signRequest(Token accessToken, OAuthRequest request) {
		signRequest(request);
	}
}
