package com.pavan.pawar;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.SSECustomerKey;
import com.amazonaws.util.Base64;
import com.amazonaws.util.Md5Utils;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class S3Test {

	private static SSECustomerKey SSE_KEY;
	private static AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
			.withCredentials(new ProfileCredentialsProvider()).build();
	private static KeyGenerator KEY_GENERATOR;
	private static SecretKey SECRET_KEY;

	private static String bucketName = "pavan-bucket";
	private static String keyName = "a.png";
	private static File uploadFileName = new File("C:\\Users\\Administrator\\Desktop\\de.png");

	public S3Test() {
		ClientConfiguration cnf = new ClientConfiguration();
		cnf.withSignerOverride("AWSS3V4SignerType");
	}

	public static void createKey() throws NoSuchAlgorithmException {
		KEY_GENERATOR = KeyGenerator.getInstance("AES");
		KEY_GENERATOR.init(256, new SecureRandom());
		SECRET_KEY = KEY_GENERATOR.generateKey();
		SSE_KEY = new SSECustomerKey(SECRET_KEY);
		System.out.println("The Key is SSE_KEY = " + SSE_KEY.getKey());
	}

	public static void uploadObject() {
		try {
			PutObjectRequest putRequest = new PutObjectRequest(bucketName, keyName, uploadFileName)
					.withSSECustomerKey(SSE_KEY);
			PutObjectResult result = s3Client.putObject(putRequest);
			System.out.println(
					"Object uploaded with algo " + result.getSSECustomerAlgorithm() + "  Using Key " + SSE_KEY);
		} catch (AmazonServiceException e) {
			// The call was transmitted successfully, but Amazon S3 couldn't
			// process
			// it, so it returned an error response.
			e.printStackTrace();
		} catch (SdkClientException e) {
			// Amazon S3 couldn't be contacted for a response, or the client
			// couldn't parse the response from Amazon S3.
			e.printStackTrace();
		}
	}

	public static void getPreSignedURL() throws URISyntaxException {
		java.util.Date expiration = new java.util.Date();
		long expTimeMillis = expiration.getTime();
		expTimeMillis += 1000 * 60 * 60;
		expiration.setTime(expTimeMillis);

		System.out.println("Generating pre-signed URL.");
		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, keyName)
				.withMethod(HttpMethod.GET).withExpiration(expiration).withSSECustomerKey(SSE_KEY);

		// generatePresignedUrlRequest.setContentType("video/mp4");
		generatePresignedUrlRequest.setSSECustomerKeyAlgorithm(SSEAlgorithm.AES256);

		URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

		System.out.println("Pre-Signed URL: " + url.toURI() + " With key: " + SSE_KEY);

		System.out.println("------------------------------");
		//https://aws.amazon.com/blogs/developer/generating-amazon-s3-pre-signed-urls-with-sse-c-part-4/ 
		// reffer above doc
		try {
			HttpResponse<String> response = Unirest.get(url.toURI().toString())
					.header(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM, SSEAlgorithm.AES256.getAlgorithm())
					.header(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY, Base64.encodeAsString(SECRET_KEY.getEncoded()))
					.header(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5,
							Md5Utils.md5AsBase64(SECRET_KEY.getEncoded()))
					.header("cache-control", "no-cache").header("postman-token", "d3453c38-1b59-a12e-fd97-dbe2150eadf5")
					.asString();

			System.out.println(response.getStatus());
			System.out.println(response.getStatusText());
			System.out.println(response.getBody());
		} catch (UnirestException e) {

			e.printStackTrace();
		}

	}

	public static void main(String args[]) {
		try {
			S3Test.createKey();
			S3Test.uploadObject();
			S3Test.getPreSignedURL();
		} catch (NoSuchAlgorithmException e) {
		
			e.printStackTrace();
		} catch (URISyntaxException e) {		
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
