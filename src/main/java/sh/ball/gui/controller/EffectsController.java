package sh.ball.gui.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import sh.ball.audio.FrequencyAnalyser;
import sh.ball.audio.ShapeAudioPlayer;
import sh.ball.audio.effect.*;
import sh.ball.audio.engine.AudioDevice;
import sh.ball.shapes.Shape;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class EffectsController implements Initializable {

  private ShapeAudioPlayer audioPlayer;

  private Map<EffectType, Slider> effectTypes;

  private final SmoothEffect smoothEffect;
  private WobbleEffect wobbleEffect;

  @FXML
  private CheckBox vectorCancellingCheckBox;
  @FXML
  private Slider vectorCancellingSlider;
  @FXML
  private SVGPath vectorCancellingMidi;
  @FXML
  private CheckBox bitCrushCheckBox;
  @FXML
  private Slider bitCrushSlider;
  @FXML
  private SVGPath bitCrushMidi;
  @FXML
  private CheckBox verticalDistortCheckBox;
  @FXML
  private Slider verticalDistortSlider;
  @FXML
  private SVGPath verticalDistortMidi;
  @FXML
  private CheckBox horizontalDistortCheckBox;
  @FXML
  private Slider horizontalDistortSlider;
  @FXML
  private SVGPath horizontalDistortMidi;
  @FXML
  private CheckBox wobbleCheckBox;
  @FXML
  private Slider wobbleSlider;
  @FXML
  private SVGPath wobbleMidi;
  @FXML
  private CheckBox smoothCheckBox;
  @FXML
  private Slider smoothSlider;
  @FXML
  private SVGPath smoothMidi;
  @FXML
  private CheckBox traceCheckBox;
  @FXML
  private Slider traceSlider;
  @FXML
  private SVGPath traceMidi;

  public EffectsController() {
    this.smoothEffect = new SmoothEffect(1);
  }

  public Map<SVGPath, Slider> getMidiButtonMap() {
    Map<SVGPath, Slider> midiMap = new HashMap<>();
    midiMap.put(vectorCancellingMidi, vectorCancellingSlider);
    midiMap.put(bitCrushMidi, bitCrushSlider);
    midiMap.put(wobbleMidi, wobbleSlider);
    midiMap.put(smoothMidi, smoothSlider);
    midiMap.put(traceMidi, traceSlider);
    midiMap.put(verticalDistortMidi, verticalDistortSlider);
    midiMap.put(horizontalDistortMidi, horizontalDistortSlider);
    return midiMap;
  }

  // Maps EffectTypes to the slider that controls the effect so that they can be
  // toggled when the appropriate checkbox is ticked.
  private void initializeEffectTypes() {
    effectTypes = Map.of(
      EffectType.VECTOR_CANCELLING,
      vectorCancellingSlider,
      EffectType.BIT_CRUSH,
      bitCrushSlider,
      EffectType.VERTICAL_DISTORT,
      verticalDistortSlider,
      EffectType.HORIZONTAL_DISTORT,
      horizontalDistortSlider,
      EffectType.WOBBLE,
      wobbleSlider,
      EffectType.SMOOTH,
      smoothSlider
    );
  }

  // selects or deselects the given audio effect
  private void updateEffect(EffectType type, boolean checked, Effect effect) {
    if (checked) {
      audioPlayer.addEffect(type, effect);
      if (effectTypes.containsKey(type)) {
        effectTypes.get(type).setDisable(false);
      }
    } else {
      audioPlayer.removeEffect(type);
      if (effectTypes.containsKey(type)) {
        effectTypes.get(type).setDisable(true);
      }
    }
  }

  public void setAudioPlayer(ShapeAudioPlayer audioPlayer) {
    this.audioPlayer = audioPlayer;
  }

  public void setAudioDevice(AudioDevice device) {
    this.wobbleEffect = new WobbleEffect(device.sampleRate());
    updateEffect(EffectType.WOBBLE, wobbleCheckBox.isSelected(), wobbleEffect);
    restartEffects();
  }

  public void setFrequencyAnalyser(FrequencyAnalyser<List<Shape>> analyser) {
    analyser.addListener(wobbleEffect);
  }

  public void restartEffects() {
    // apply the wobble effect after a second as the frequency of the audio takes a while to
    // propagate and send to its listeners.
    KeyFrame kf1 = new KeyFrame(Duration.seconds(0), e -> wobbleEffect.setVolume(0));
    KeyFrame kf2 = new KeyFrame(Duration.seconds(1), e -> {
      wobbleEffect.update();
      wobbleEffect.setVolume(wobbleSlider.getValue());
    });
    Timeline timeline = new Timeline(kf1, kf2);
    Platform.runLater(timeline::play);
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    initializeEffectTypes();

    InvalidationListener vectorCancellingListener = e ->
      updateEffect(EffectType.VECTOR_CANCELLING, vectorCancellingCheckBox.isSelected(),
        EffectFactory.vectorCancelling((int) vectorCancellingSlider.getValue()));
    InvalidationListener bitCrushListener = e ->
      updateEffect(EffectType.BIT_CRUSH, bitCrushCheckBox.isSelected(),
        EffectFactory.bitCrush(bitCrushSlider.getValue()));
    InvalidationListener verticalDistortListener = e ->
      updateEffect(EffectType.VERTICAL_DISTORT, verticalDistortCheckBox.isSelected(),
        EffectFactory.verticalDistort(verticalDistortSlider.getValue()));
    InvalidationListener horizontalDistortListener = e ->
      updateEffect(EffectType.HORIZONTAL_DISTORT, horizontalDistortCheckBox.isSelected(),
        EffectFactory.horizontalDistort(horizontalDistortSlider.getValue()));
    InvalidationListener wobbleListener = e -> {
      wobbleEffect.setVolume(wobbleSlider.getValue());
      updateEffect(EffectType.WOBBLE, wobbleCheckBox.isSelected(), wobbleEffect);
    };
    InvalidationListener smoothListener = e -> {
      smoothEffect.setWindowSize((int) smoothSlider.getValue());
      updateEffect(EffectType.SMOOTH, smoothCheckBox.isSelected(), smoothEffect);
    };
    InvalidationListener traceListener = e -> {
      double trace = traceCheckBox.isSelected() ? traceSlider.valueProperty().getValue() : 1;
      audioPlayer.setTrace(trace);
      traceSlider.setDisable(!traceCheckBox.isSelected());
    };

    vectorCancellingSlider.valueProperty().addListener(vectorCancellingListener);
    vectorCancellingCheckBox.selectedProperty().addListener(vectorCancellingListener);

    bitCrushSlider.valueProperty().addListener(bitCrushListener);
    bitCrushCheckBox.selectedProperty().addListener(bitCrushListener);

    verticalDistortSlider.valueProperty().addListener(verticalDistortListener);
    verticalDistortCheckBox.selectedProperty().addListener(verticalDistortListener);

    horizontalDistortSlider.valueProperty().addListener(horizontalDistortListener);
    horizontalDistortCheckBox.selectedProperty().addListener(horizontalDistortListener);

    wobbleSlider.valueProperty().addListener(wobbleListener);
    wobbleCheckBox.selectedProperty().addListener(wobbleListener);
    wobbleCheckBox.selectedProperty().addListener(e -> wobbleEffect.update());

    smoothSlider.valueProperty().addListener(smoothListener);
    smoothCheckBox.selectedProperty().addListener(smoothListener);

    traceSlider.valueProperty().addListener(traceListener);
    traceCheckBox.selectedProperty().addListener(traceListener);
  }

  public List<CheckBox> effectCheckBoxes() {
    return List.of(vectorCancellingCheckBox, bitCrushCheckBox, verticalDistortCheckBox,
      horizontalDistortCheckBox, wobbleCheckBox, smoothCheckBox, traceCheckBox);
  }
  public List<Slider> effectSliders() {
    return List.of(vectorCancellingSlider, bitCrushSlider, verticalDistortSlider,
      horizontalDistortSlider, wobbleSlider, smoothSlider, traceSlider);
  }
}
