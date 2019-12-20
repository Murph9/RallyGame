package test;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.DefaultRangedValueModel;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.PasswordField;
import com.simsilica.lemur.Slider;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.text.DocumentModelFilter;
import com.simsilica.lemur.text.TextFilters;

import duel.DuelUiStyle;
import helper.Log;
import helper.Screen;

class DuelUiTestApp extends SimpleApplication {

    public static void main(String[] args) {
        DuelUiTestApp app = new DuelUiTestApp();
        app.setDisplayStatView(true);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        inputManager.setCursorVisible(true);
        inputManager.deleteMapping(INPUT_MAPPING_EXIT); // no esc close pls

        // initialize Lemur (the GUI manager)
        GuiGlobals.initialize(this);
        // Load my duel Lemur style
        DuelUiStyle.load(assetManager);

        getStateManager().attach(new DuelUIStyleTest());
    }
}

public class DuelUIStyleTest extends BaseAppState {

    // copy UI stuff to test with from here:
    // https://github.com/jMonkeyEngine-Contributions/Lemur/tree/master/examples/demos/src/main/java/demo

    private List<Container> windows;

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize(Application app) {
        windows = new LinkedList<Container>();

        Container window = new Container();
        window.addChild(new Label("Panel Alpha Demo", new ElementId("window.title")));
        window.addChild(new Label("Drag the slider for fun and profit:"));
        window.addChild(new Slider(new DefaultRangedValueModel(0.1, 1, 0.5)));

        window.addChild(new Label("Filtered input tests:"));
        Container examples = window.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.Even, FillMode.Last)));

        // Add an alpha example
        DocumentModelFilter doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.alpha());
        examples.addChild(new Label("Alpha only:"));
        TextField textField = examples.addChild(new TextField(doc), 1);
        textField.setPreferredWidth(300);

        // Add a numeric example
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        examples.addChild(new Label("Numeric only:"));
        textField = examples.addChild(new TextField(doc), 1);

        // A new subsection for the output filters
        window.addChild(new Label("Filtered Output:"));
        examples = window.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.Even, FillMode.Last)));

        // Add an all-caps example
        doc = new DocumentModelFilter();
        doc.setOutputTransform(TextFilters.upperCaseTransform());
        examples.addChild(new Label("All Caps:"));
        textField = examples.addChild(new TextField(doc), 1);
        examples.addChild(new Label("-> unfiltered:"));
        textField = examples.addChild(new TextField(doc.getDelegate()), 1);

        // Add a constant-char example
        doc = new DocumentModelFilter();
        doc.setOutputTransform(TextFilters.constantTransform('*'));
        examples.addChild(new Label("Obscured:"));
        textField = examples.addChild(new TextField(doc), 1);
        examples.addChild(new Label("-> unfiltered:"));
        textField = examples.addChild(new TextField(doc.getDelegate()), 1);

        // A new subsection for the built in filtered text support like PasswordField
        window.addChild(new Label("Standard Elements:"));
        examples = window.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.Even, FillMode.Last)));

        PasswordField pword;

        // Add a PasswordField example
        examples.addChild(new Label("Password:"));
        pword = examples.addChild(new PasswordField(""), 1);
        examples.addChild(new Label("-> unfiltered:"));
        textField = examples.addChild(new TextField(pword.getDocumentModel()), 1);

        // Add a PasswordField example
        examples.addChild(new Label("Password Alt:"));
        pword = examples.addChild(new PasswordField(""), 1);
        pword.setOutputCharacter('#');
        examples.addChild(new Label("-> unfiltered:"));
        textField = examples.addChild(new TextField(pword.getDocumentModel()), 1);

        // Add a PasswordField example
        // Note: don't do this in real life unless you want easily hackable passwords
        examples.addChild(new Label("Alpha-numeric Password:"));
        pword = examples.addChild(new PasswordField(""), 1);
        pword.setAllowedCharacters(TextFilters.isLetterOrDigit());
        examples.addChild(new Label("-> unfiltered:"));
        textField = examples.addChild(new TextField(pword.getDocumentModel()), 1);

        Button button = examples.addChild(new Button("Close"));
        button.addClickCommands(new Command<Button>() {
            @Override
            public void execute(Button source) {
                getApplication().getStateManager().detach(getState(DuelUIStyleTest.class));
            }
        });

        windows.add(window);

        Screen screen = new Screen(app.getContext().getSettings());
        for (Container w: windows) {
            ((SimpleApplication) app).getGuiNode().attachChild(w);
            screen.centerMe(w);
        }

        Log.p("DuelUIStyleTest start");
    }

    @Override
    protected void cleanup(Application app) {
        for (Container w: windows) {
            ((SimpleApplication) app).getGuiNode().detachChild(w);
        }
        Log.p("DuelUIStyleTest end");
    }

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}

}
