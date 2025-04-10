package titanicsend.model;

import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import java.util.List;
import java.util.stream.Stream;

public abstract class TEModel {
  private final String teModelType;
  private String id;
  public final LXModel model;

  /** Static model constructor (2022-23) */
  public TEModel(String teModelType, List<LXPoint> points, String... tags) {
    this.teModelType = teModelType;
    this.model = new LXModel(points, combineTags(teModelType, tags));
  }

  private static String[] combineTags(String tag0, String... tags) {
    return Stream.concat(Stream.of(tag0), Stream.of(tags))
        .filter(tag -> tag != null)
        .toArray(String[]::new);
  }

  /** Dynamic model constructor (2024+) */
  public TEModel(String teModelType, LXModel model) {
    this.teModelType = teModelType;
    this.model = model;
  }

  /** Child classes should call to set the TE id */
  protected void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  public String repr() {
    return teModelType + "_" + this.getId();
  }
}
