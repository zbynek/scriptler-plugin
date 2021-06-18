package org.jenkinsci.plugins.scriptler;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;
import static org.hamcrest.CoreMatchers.is;

public class ScriptlerEncodingTest {
    private static final String SCRIPT_ID = "encodingTest.groovy";
    private static final String INTERNATIONALIZED_SCRIPT = "def myString = '3.2.0\u00df1'\n" +
            "println myString.replaceAll(/(\u00df|\\ RC\\ )/,'.')\n";
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static Charset previousDefaultCharset;

    @Before
    public void overwriteDefaultCharset() throws Exception {
        Field defaultCharset = Charset.class.getDeclaredField("defaultCharset");
        defaultCharset.setAccessible(true);
        previousDefaultCharset = (Charset) defaultCharset.get(null);
        defaultCharset.set(null, StandardCharsets.ISO_8859_1);
        assumeThat(Charset.defaultCharset().name(), is("ISO-8859-1"));
    }

    @After
    public void restoreDefaultCharset() throws Exception {
        Field defaultCharset = Charset.class.getDeclaredField("defaultCharset");
        defaultCharset.setAccessible(true);
        defaultCharset.set(null, previousDefaultCharset);
    }

    @Test
    @Issue("JENKINS-59841")
    public void testNonAsciiEncodingSaving() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();

        HtmlPage scriptAddPage = wc.goTo("scriptler/scriptSettings");
        HtmlForm scriptAddForm = scriptAddPage.getFormByName("scriptAdd");
        ((HtmlTextInput) scriptAddForm.getInputByName("id")).setText(SCRIPT_ID);
        ((HtmlTextInput) scriptAddForm.getInputByName("name")).setText("Encoding Test");
        scriptAddForm.getInputByName("nonAdministerUsing").setChecked(true);
        scriptAddForm.getTextAreaByName("script").setText(INTERNATIONALIZED_SCRIPT);

        HtmlPage scriptAddPage1 = j.submit(scriptAddForm);
        j.assertGoodStatus(scriptAddPage1);

        HtmlPage showScriptPage = wc.goTo("scriptler/showScript?id=" + SCRIPT_ID);
        j.assertGoodStatus(showScriptPage);
        HtmlTextArea script = showScriptPage.getElementByName("script");
        assertEquals(INTERNATIONALIZED_SCRIPT, script.getText());
    }
}
