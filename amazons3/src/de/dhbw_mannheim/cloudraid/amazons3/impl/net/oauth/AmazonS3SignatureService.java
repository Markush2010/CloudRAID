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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.scribe.exceptions.OAuthSignatureException;
import org.scribe.services.SignatureService;

import com.miginfocom.base64.Base64;

/**
 * HMAC-SHA1 implementation of {@SignatureService}
 * 
 * @author Pablo Fernandez
 * 
 */
public class AmazonS3SignatureService implements SignatureService {

	/**
	 * Just for easier writing
	 */
	private static final String UTF8 = "UTF-8";

	/**
	 * The Javax.crypto name of HMAC_SHA1
	 */
	private static final String HMAC_SHA1 = "HmacSHA1";

	/**
	 * The printable version of HMAC_SHA1
	 */
	private static final String METHOD = "HMAC-SHA1";

	/**
	 * @param toSign
	 *            The string that will be signed
	 * @param keyString
	 *            The key used for signing
	 * @return The signed string
	 * @throws Exception
	 *             Thrown, if the signing fails
	 */
	private String doSign(String toSign, String keyString) throws Exception {
		SecretKeySpec key = new SecretKeySpec(
				(keyString).getBytes(AmazonS3SignatureService.UTF8),
				AmazonS3SignatureService.HMAC_SHA1);
		Mac mac = Mac.getInstance(AmazonS3SignatureService.HMAC_SHA1);
		mac.init(key);
		byte[] bytes = mac.doFinal(toSign
				.getBytes(AmazonS3SignatureService.UTF8));
		return Base64.encodeToString(bytes, false);
	}

	/**
	 * @param baseString
	 *            The string to sign
	 * @param secretKey
	 *            The secret key to use for signing
	 * @return Returns the signature
	 */
	public String getSignature(String baseString, String secretKey) {
		try {
			return doSign(baseString, secretKey);
		} catch (Exception e) {
			throw new OAuthSignatureException(baseString, e);
		}
	}

	/**
	 * Just to fulfill the requirements of the interface! <b>Not used!</b>
	 */
	@Override
	public String getSignature(String baseString, String apiSecret,
			String tokenSecret) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSignatureMethod() {
		return AmazonS3SignatureService.METHOD;
	}
}
