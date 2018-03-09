/**
 * A HTTP plugin for Cordova / Phonegap
 */
package com.synconset.cordovahttp;

import android.util.Base64;
import com.github.kevinsawicki.http.HttpRequest;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class CordovaHttpPlugin extends CordovaPlugin {
  private static final String TAG = "CordovaHTTP";
  private static final String[] in = {"encarr"};

  @Override
  public void initialize( CordovaInterface cordova, CordovaWebView webView ) {
    super.initialize( cordova, webView );
  }

  @Override
  public boolean execute( String action, final JSONArray args, final CallbackContext callbackContext ) throws JSONException {
    if ( action.equals( "post" ) ) {
      String urlString = args.getString( 0 );
      Object params = args.get( 1 );
      String serializerName = args.getString( 2 );
      JSONObject headers = args.getJSONObject( 3 );
      int timeoutInMilliseconds = args.getInt( 4 ) * 1000;
      CordovaHttpPost post = new CordovaHttpPost( urlString, params, serializerName, headers, timeoutInMilliseconds, callbackContext );

      cordova.getThreadPool().execute( post );
    } else if ( action.equals( "get" ) ) {
      String urlString = args.getString( 0 );
      Object params = args.get( 1 );
      JSONObject headers = args.getJSONObject( 2 );
      int timeoutInMilliseconds = args.getInt( 3 ) * 1000;
      CordovaHttpGet get = new CordovaHttpGet( urlString, params, headers, timeoutInMilliseconds, callbackContext );

      cordova.getThreadPool().execute( get );
    } else if ( action.equals( "put" ) ) {
      String urlString = args.getString( 0 );
      Object params = args.get( 1 );
      String serializerName = args.getString( 2 );
      JSONObject headers = args.getJSONObject( 3 );
      int timeoutInMilliseconds = args.getInt( 4 ) * 1000;
      CordovaHttpPut put = new CordovaHttpPut( urlString, params, serializerName, headers, timeoutInMilliseconds, callbackContext );

      cordova.getThreadPool().execute( put );
    } else if ( action.equals( "patch" ) ) {
      String urlString = args.getString( 0 );
      Object params = args.get( 1 );
      String serializerName = args.getString( 2 );
      JSONObject headers = args.getJSONObject( 3 );
      int timeoutInMilliseconds = args.getInt( 4 ) * 1000;
      CordovaHttpPatch patch = new CordovaHttpPatch( urlString, params, serializerName, headers, timeoutInMilliseconds, callbackContext );

      cordova.getThreadPool().execute( patch );
    } else if ( action.equals( "delete" ) ) {
      String urlString = args.getString( 0 );
      Object params = args.get( 1 );
      JSONObject headers = args.getJSONObject( 2 );
      int timeoutInMilliseconds = args.getInt( 3 ) * 1000;
      CordovaHttpDelete delete = new CordovaHttpDelete( urlString, params, headers, timeoutInMilliseconds, callbackContext );

      cordova.getThreadPool().execute( delete );
    } else if ( action.equals( "head" ) ) {
      String urlString = args.getString( 0 );
      Object params = args.get( 1 );
      JSONObject headers = args.getJSONObject( 2 );
      int timeoutInMilliseconds = args.getInt( 3 ) * 1000;
      CordovaHttpHead head = new CordovaHttpHead( urlString, params, headers, timeoutInMilliseconds, callbackContext );

      cordova.getThreadPool().execute( head );
    } else if ( action.equals( "enableSSLPinning" ) ) {
      try {
        boolean enable = args.getBoolean( 0 );
        this.enableSSLPinning( enable );
        callbackContext.success();
      } catch ( Exception e ) {
        e.printStackTrace();
        callbackContext.error( "There was an error setting up ssl pinning" );
      }
    } else if ( action.equals( "acceptAllCerts" ) ) {
      boolean accept = args.getBoolean( 0 );

      CordovaHttp.acceptAllCerts( accept );
      CordovaHttp.validateDomainName( !accept );
      callbackContext.success();
    } else if ( action.equals( "uploadFile" ) ) {
      String urlString = args.getString( 0 );
      Object params = args.get( 1 );
      JSONObject headers = args.getJSONObject( 2 );
      String filePath = args.getString( 3 );
      String name = args.getString( 4 );
      int timeoutInMilliseconds = args.getInt( 5 ) * 1000;
      CordovaHttpUpload upload = new CordovaHttpUpload( urlString, params, headers, filePath, name, timeoutInMilliseconds, callbackContext );

      cordova.getThreadPool().execute( upload );
    } else if ( action.equals( "downloadFile" ) ) {
      String urlString = args.getString( 0 );
      Object params = args.get( 1 );
      JSONObject headers = args.getJSONObject( 2 );
      String filePath = args.getString( 3 );
      int timeoutInMilliseconds = args.getInt( 4 ) * 1000;
      CordovaHttpDownload download = new CordovaHttpDownload( urlString, params, headers, filePath, timeoutInMilliseconds, callbackContext );

      cordova.getThreadPool().execute( download );
    } else if ( action.equals( "disableRedirect" ) ) {
      boolean disable = args.getBoolean( 0 );
      CordovaHttp.disableRedirect( disable );
      callbackContext.success();
    } else {
      return false;
    }
    return true;
  }

  private void enableSSLPinning( boolean enable ) throws Exception {
    if ( enable ) {
      try {
        for ( String s : in ) {
          HttpRequest.addCert( toBytes(s) );
        }
      } finally {
        CordovaHttp.enableSSLPinning( true );
      }
    } else {
      CordovaHttp.enableSSLPinning( false );
    }
  }

  private ByteArrayInputStream toBytes( String input ) throws Exception {
    String ck = "ck";
    String c4 = "c4";
    byte[] bytes = Base64.decode( input, Base64.DEFAULT );

    SecretKey skey = new SecretKeySpec( ck.getBytes( "UTF-8" ), "AES" );
    Cipher cipher = Cipher.getInstance( "AES/CBC/PKCS5Padding" );
    cipher.init( Cipher.DECRYPT_MODE, skey, new IvParameterSpec( c4.getBytes( "UTF-8" ) ) );

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    bos.write( cipher.doFinal( bytes ) );
    return new ByteArrayInputStream( bos.toByteArray() );

  }
}
