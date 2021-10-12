package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.example.otel.AgentClassLoader;
import org.example.otel.InternalJarUrlHandler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class CyclicGetter {

    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, ClassNotFoundException, MalformedURLException {

        File jar = new File("opentelemetry-javaagent-1.1.0-SNAPSHOT-all.jar");
        URL jarUrl = jar.toURI().toURL();

        ClassLoader jarClassLoader = new URLClassLoader(new URL[]{jarUrl}, CyclicGetter.class.getClassLoader());

        InternalJarUrlHandler internalJarUrlHandler = new InternalJarUrlHandler("inst", jarUrl);

        AgentClassLoader agentClassLoader = new AgentClassLoader(jarUrl, "inst", ClassLoader.getSystemClassLoader());
        Class<?> loadedClass = agentClassLoader.loadClass("io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.ApacheHttpClientTracer");
        Thread.currentThread().setContextClassLoader(agentClassLoader); //TODO: check if this line works

        if(loadedClass != null){
            System.out.println("CLASS LOADED SUCCESSFULLY");
        }

        //somehow show loaded class to

        OtelConfig.configureOtel();

        while(true){
            try {
                makeCall();
                Thread.sleep(2000);
            }
            catch (IOException e) {
                e.printStackTrace();
                System.out.println();
            } catch (InterruptedException e) {
                System.out.println("Problem with sleep :(");
                e.printStackTrace();
            }
        }

    }

    private static void makeCall() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {

            HttpGet request = new HttpGet("https://httpbin.org/get");

            // add request headers
            request.addHeader("custom-key", "mkyong");
            request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");

            CloseableHttpResponse response = httpClient.execute(request);

            try {

                // Get HttpResponse Status
                System.out.println(response.getProtocolVersion());              // HTTP/1.1
                System.out.println(response.getStatusLine().getStatusCode());   // 200
                System.out.println(response.getStatusLine().getReasonPhrase()); // OK
                System.out.println(response.getStatusLine().toString());        // HTTP/1.1 200 OK

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // return it as a String
                    String result = EntityUtils.toString(entity);
                    System.out.println(result);
                }

            } finally {
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

}