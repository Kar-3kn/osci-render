package sh.ball.engine;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ObjectServer implements Runnable {

  private static final int PORT = 51677;
  private final Gson gson = new Gson();
  private final Map<String, WorldObject> objects = new HashMap<>();
  private final ObjectSet objectSet = new ObjectSet();
  private final Runnable enableRendering;
  private final Runnable disableRendering;

  public ObjectServer(Runnable enableRendering, Runnable disableRendering) {
    this.enableRendering = enableRendering;
    this.disableRendering = disableRendering;
  }

  @Override
  public void run() {
    try {
      ServerSocket Server = new ServerSocket(PORT);

      while (true) {
        Socket socket = Server.accept();
        enableRendering.run();
        BufferedReader clientReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        while (socket.isConnected()) {
          String json = clientReader.readLine();
          if (json.equals("CLOSE")) {
            socket.close();
            break;
          }
          EngineInfo info = gson.fromJson(json, EngineInfo.class);

          List<WorldObject> objectsToRender = new ArrayList<>();
          List<float[]> objectMatrices = new ArrayList<>();

          List<Vector3[]> pathObjects = new ArrayList<>();
          List<float[]> pathMatrices = new ArrayList<>();

          Set<String> currentObjects = new HashSet<>();

          for (ObjectInfo obj : info.objects) {
            currentObjects.add(obj.name);
            if (!objects.containsKey(obj.name)) {
              if (obj.vertices != null) {
                objects.put(obj.name, new WorldObject(obj.vertices, obj.edges, obj.faces));
              }
            }
            if (obj.pathVertices == null) {
              objectsToRender.add(objects.get(obj.name));
              objectMatrices.add(obj.matrix);
            } else {
              pathObjects.add(obj.pathVertices);
              pathMatrices.add(obj.matrix);
            }
          }

          objects.entrySet().removeIf(obj -> !currentObjects.contains(obj.getKey()));

          objectSet.setObjects(objectsToRender, objectMatrices, pathObjects, pathMatrices, info.focalLength);
        }
        disableRendering.run();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ObjectSet getObjectSet() {
    return objectSet;
  }

  private static class EngineInfo {
    private ObjectInfo[] objects;
    private float focalLength;

    @Override
    public String toString() {
      return "EngineInfo{" +
        "objects=" + Arrays.toString(objects) +
        "focalLength=" + focalLength +
        '}';
    }
  }

  private static class ObjectInfo {
    private String name;
    private Vector3[] vertices;
    private Vector3[] pathVertices;
    private int[] edges;
    private int[][] faces;
    // Camera space matrix
    private float[] matrix;

    @Override
    public String toString() {
      return "ObjectInfo{" +
        "name='" + name + '\'' +
        ", vertices=" + Arrays.toString(vertices) +
        ", edges=" + Arrays.toString(edges) +
        ", faces=" + Arrays.deepToString(faces) +
        ", matrix=" + Arrays.toString(matrix) +
        '}';
    }
  }
}
