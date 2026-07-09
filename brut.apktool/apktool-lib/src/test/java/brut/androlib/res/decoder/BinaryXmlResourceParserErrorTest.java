package brut.androlib.res.decoder;

import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResPackage;
import brut.androlib.res.table.ResPackageGroup;
import brut.androlib.res.table.ResTable;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BinaryXmlResourceParserErrorTest {

    @Test
    public void testFirstErrorCapturedWhenResolvingFails() throws Exception {
        ApkInfo apkInfo = new ApkInfo();
        Config config = new Config("1.0");
        ResTable table = new ResTable(apkInfo, config);

        ResPackageGroup group = new ResPackageGroup(table, 0x7f, "testGroup");
        ResPackage pkg = group.getBasePackage();

        Field mainPkgField = ResTable.class.getDeclaredField("mMainPackage");
        mainPkgField.setAccessible(true);
        mainPkgField.set(table, pkg);

        BinaryXmlResourceParser parser = new BinaryXmlResourceParser(table, false, false);

        Field eventTypeField = BinaryXmlResourceParser.class.getDeclaredField("mEventType");
        eventTypeField.setAccessible(true);
        eventTypeField.set(parser, 2); // XmlPullParser.START_TAG = 2

        Class<?> attrClass = Class.forName("brut.androlib.res.decoder.BinaryXmlResourceParser$Attribute");
        Constructor<?> attrConstructor = attrClass.getDeclaredConstructor(int.class, int.class, int.class, int.class, int.class);
        attrConstructor.setAccessible(true);
        Object attr = attrConstructor.newInstance(-1, 0, -1, 0, 0); // ns = -1, name = 0, rawValue = -1, valueType = 0, valueData = 0

        Object array = java.lang.reflect.Array.newInstance(attrClass, 1);
        java.lang.reflect.Array.set(array, 0, attr);

        Field attributesField = BinaryXmlResourceParser.class.getDeclaredField("mAttributes");
        attributesField.setAccessible(true);
        attributesField.set(parser, array);

        ResId[] resMap = new ResId[] { ResId.of(0x7f, 0x01, 0x0001) };
        Field resourceMapField = BinaryXmlResourceParser.class.getDeclaredField("mResourceMap");
        resourceMapField.setAccessible(true);
        resourceMapField.set(parser, resMap);

        pkg.addTypeSpec(0x01, "existing");
        pkg.addEntrySpec(0x01, 0x0001, "some_entry");

        parser.getAttributeName(0);

        assertNotNull("First error should be captured", parser.getFirstError());
        assertTrue("Error should be an AndrolibException", parser.getFirstError() instanceof AndrolibException);
    }
}
