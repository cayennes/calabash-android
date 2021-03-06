package sh.calaba.instrumentationbackend.actions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Properties;

import android.graphics.Bitmap;
import android.view.View;
import sh.calaba.instrumentationbackend.Command;
import sh.calaba.instrumentationbackend.InstrumentationBackend;
import sh.calaba.instrumentationbackend.Result;
import sh.calaba.org.codehaus.jackson.map.DeserializationConfig.Feature;
import sh.calaba.org.codehaus.jackson.map.ObjectMapper;
import android.util.Log;

public class HttpServer extends NanoHTTPD {
	private static final String TAG = "IntrumentationBackend";
	private boolean running = true;
	
	private final ObjectMapper mapper = createJsonMapper();

	public HttpServer() {
		super(7102, new File("/"));
	}

	public Response serve( String uri, String method, Properties header, Properties params, Properties files )
	{
		System.out.println("URI: " + uri);
		if (uri.endsWith("/ping")) {
			return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, "pong");
		} else if (uri.endsWith("/kill")) {
			running = false;
			System.out.println("Stopping test server");
			stop();
			return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, "Affirmative!");

		} else if (uri.endsWith("/screenshot")) {
            Bitmap bitmap;
            View v1 = InstrumentationBackend.solo.getViews().get(0).getRootView();
            v1.setDrawingCacheEnabled(true);
            bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            return new NanoHTTPD.Response( HTTP_OK, "image/png", new ByteArrayInputStream(out.toByteArray()));
        }
		
		String commandString = params.getProperty("command");
		System.out.println("command: "+ commandString);
		String result = toJson(runCommand(commandString));
		System.out.println("result:" + result);
		
		return new NanoHTTPD.Response( HTTP_OK, MIME_HTML, result);
	}

	private ObjectMapper createJsonMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, true);
		return mapper;
	}

	private String toJson(Result result) {
		try {
			return mapper.writeValueAsString(result);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Result runCommand(String commandString) {
		try {
			Command command = mapper.readValue(commandString, Command.class);
			log("Got command:'" + command);
			return command.execute();
		} catch (Throwable t) {
			t.printStackTrace();
			return Result.fromThrowable(t);
		}
	}

	public boolean isRunning() {
		return running;
	}

	public static void log(String message) {
		Log.i(TAG, message);
	}
}
