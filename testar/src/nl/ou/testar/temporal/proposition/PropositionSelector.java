package nl.ou.testar.temporal.proposition;

import nl.ou.testar.StateModel.Persistence.OrientDB.Util.Validation;
import nl.ou.testar.temporal.util.CachedRegexPatterns;
import nl.ou.testar.temporal.foundation.PairBean;
import nl.ou.testar.temporal.foundation.TagBean;
import org.fruit.alayer.Shape;
import org.fruit.alayer.Tag;
import org.fruit.alayer.Tags;
import org.fruit.alayer.windows.UIARoles;
import org.fruit.alayer.windows.UIATags;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropositionSelector {
    private Set<String> selectedAttributes;
    private Set<PairBean<InferrableExpression,String>> selectedExpressions;



    public PropositionSelector() {

        selectedAttributes=new LinkedHashSet<>();
        selectedExpressions =new LinkedHashSet<>();

    }


    public static Set<String> getAllAttributeNames(){
        Set<TagBean<?>> tags = getAllAttributeTags();
        Set<String> tmptagset=new LinkedHashSet<>();
        Iterator<TagBean<?>> iterator;
        for (iterator = tags.iterator(); iterator.hasNext(); ) {
            TagBean<?> t = iterator.next();
            tmptagset.add(t.name());
        }
        tmptagset.addAll(useMinimalAttributes());
        return  tmptagset;
    }
    @SuppressWarnings("unused")
    public static Set<TagBean<?>> getAllAttributeTags(){
        // WORKAROUND CSS 20190629
        // the 2 dummy reads are required to ensure properly initialization of the classes: static method/property is used!
        // both classes Tags and UIATags inherit from abstract class TagBase
        // without this dual initialization, the call to .tagset() from either class collides into the same tagset content.
        // symptom: 'UIATags' appears to have the same tags as 'Tags' and we're missing out on the real 'UIATags'.
        Tag<?> dummy = UIATags.UIAItemType;
        dummy= Tags.Enabled;
        Set<Tag<?>> tags = new HashSet<>();
        tags.addAll(Tags.tagSet());
        tags.addAll(UIATags.tagSet());//alternative for platform independent is : getNativetags ??
        Set<TagBean<?>> tmptagset=new LinkedHashSet<>();
        Iterator<Tag<?>> iterator;
        for (iterator = tags.iterator(); iterator.hasNext(); ) {
            Tag<?> t = iterator.next();
            TagBean<?> t1 = TagBean.from(Validation.sanitizeAttributeName(t.name()), t.type()); //orientdb style tags
            tmptagset.add(t1);
        }
        tmptagset.add(TagBean.IsTerminalState);
        return  tmptagset;
    }

    public static Set<String> useMinimalAttributes(){
        Set<String> tmptagset=new LinkedHashSet<>();
        Tag<?> t = Tags.Role;// is the only attribute existing for ALL states.
        tmptagset.add(Validation.sanitizeAttributeName(t.name())); //orientdb style tags
        return  tmptagset;
    }
    public static Set<String> useMinimalTransAttributes(){
        Set<String> tmptagset=new LinkedHashSet<>();
        Tag<?> t = Tags.Desc;// is the only attribute existing for ALL transactions. (e.g.ROLE doesn't exist for a Virtual Key)
        tmptagset.add(Validation.sanitizeAttributeName(t.name())); //orientdb style tags
        return  tmptagset;
    }

    public static Set<String> useBasicAttributes() {
        Set<String> tmptagset=new LinkedHashSet<>();
        Set<String> basicset = new HashSet<>();
        basicset.add(Tags.Title.name());
        basicset.add(Tags.Path.name());
        basicset.add(Tags.Role.name());
        basicset.add(Tags.Desc.name());
        basicset.add(Tags.ZIndex.name());
        basicset.add(Tags.Blocked.name());
        basicset.add(Tags.Shape.name());

        for (String tag : getAllAttributeNames()
        ) {
            if (basicset.contains(tag)) {
                tmptagset.add(tag);
            }
        }
        tmptagset.addAll(useMinimalAttributes());
        return tmptagset;
    }


    public static Set<PairBean<InferrableExpression,String>> useMinimalSelectedExpressions() {
        Set<PairBean<InferrableExpression, String>> minve = new LinkedHashSet<>();
        minve.add(new PairBean<>(InferrableExpression.exists, ""));// use always
        return minve;
    }
    public static Set<PairBean<InferrableExpression,String>> useMinimalTransSelectedExpressions() {
        // following list is derived by inspecting AnnotatingActionCompiler
        Set<PairBean<InferrableExpression, String>> minie = new LinkedHashSet<>();
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:Left Click).*"));
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:Right Click).*"));
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:Drag).*"));
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:Left Double Click).*"));
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:Type ).*(?i:into ).*"));
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:Replace ').*"));
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:Append ').*"));
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:Hit Shortcut Key).*"));
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:Hit Key).*"));
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:Bring the system to the foreground).*"));
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i: Kill Process).*"));
        return minie;
    }



    public static Set<PairBean<InferrableExpression,String>> useRoleConditionalExpressions() {
        Set<PairBean<InferrableExpression, String>> minie = new LinkedHashSet<>();
        minie.add(new PairBean<>(InferrableExpression.textmatch, UIARoles.UIAButton.name()+"|"+
                UIARoles.UIAMenu.name()+"|"+ UIARoles.UIAMenuItem.name()+"|"+ UIARoles.UIACheckBox.name()+"|"+
                UIARoles.UIARadioButton.name()+"|"+ UIARoles.UIAWindow.name()));  // use always
        return minie;
    }
    public static Set<PairBean<InferrableExpression,String>> usePathConditionalExpressions() {
        Set<PairBean<InferrableExpression, String>> minie = new LinkedHashSet<>();
       minie.add(new PairBean<>(InferrableExpression.textmatch, ".*\\[(\\d+, )*\\d+\\]"));// use always
        return minie;

    }

    public static Set<PairBean<InferrableExpression, String>> useTitleConditionalExpressions() {
        Set<PairBean<InferrableExpression, String>> minie = new LinkedHashSet<>();
        minie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:OK)|(?i:CANCEL)|(?i:YES)|(?i:NO)|" +
                "(?i:RUN)|(?i:SAVE)|(?i:EXIT)|(?i:CLOSE)|(?i:REMOVE)|(?i:ERROR)|" +
                "(?i:SUBMIT)|(?i:OPEN)|(?i:IGNORE)|(?i:PROCEED)|(?i:PRINT)|(?i:VIEW)|" +
                "(?i:UP)|(?i:DOWN)|(?i:LEFT)|(?i:RIGHT)"));
        return minie;
    }
    //

    public static Set<PairBean<InferrableExpression,String>> useBasicSelectedExpressions() {
        Set<PairBean<InferrableExpression, String>> defie = new LinkedHashSet<>(useMinimalSelectedExpressions());
        defie.add(new PairBean<>(InferrableExpression.value_eq, "0"));
        defie.add(new PairBean<>(InferrableExpression.value_eq, "1"));
        defie.add(new PairBean<>(InferrableExpression.value_eq, "2"));
        defie.add(new PairBean<>(InferrableExpression.value_lt, "10"));
        defie.add(new PairBean<>(InferrableExpression.value_lt, "100"));
        defie.add(new PairBean<>(InferrableExpression.value_lt, "1000"));
        defie.add(new PairBean<>(InferrableExpression.value_lt, "10000"));
        defie.add(new PairBean<>(InferrableExpression.value_lt, "100000"));
        defie.add(new PairBean<>(InferrableExpression.value_lt, "1000000"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:OK)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:CANCEL)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:YES)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:NO)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:GO)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:RUN)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:SAVE)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:EXIT)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:CLOSE)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:REMOVE)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:ERROR)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:SUBMIT)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:OPEN)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:IGNORE)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:PROCEED)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:PRINT)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:VIEW)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:UP)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:DOWN)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:LEFT)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "(?i:RIGHT)"));
        defie.add(new PairBean<>(InferrableExpression.textmatch, "")); //no title
        defie.add(new PairBean<>(InferrableExpression.textmatch, ".*\\[(\\d+, )*\\d+\\]"));
        defie.add(new PairBean<>(InferrableExpression.heigth_lt, "50"));
        defie.add(new PairBean<>(InferrableExpression.heigth_lt, "250"));
        defie.add(new PairBean<>(InferrableExpression.heigth_lt, "500"));
        defie.add(new PairBean<>(InferrableExpression.heigth_lt, "1000"));
        defie.add(new PairBean<>(InferrableExpression.width_lt, "50"));
        defie.add(new PairBean<>(InferrableExpression.width_lt, "250"));
        defie.add(new PairBean<>(InferrableExpression.width_lt, "500"));
        defie.add(new PairBean<>(InferrableExpression.width_lt, "1000"));
        defie.add(new PairBean<>(InferrableExpression.textlength_eq, "1"));
        defie.add(new PairBean<>(InferrableExpression.textlength_eq, "2"));
        defie.add(new PairBean<>(InferrableExpression.textlength_eq, "3"));
        defie.add(new PairBean<>(InferrableExpression.textlength_lt, "10"));
        defie.add(new PairBean<>(InferrableExpression.textlength_lt, "20"));
        defie.add(new PairBean<>(InferrableExpression.textlength_lt, "50"));
        defie.add(new PairBean<>(InferrableExpression.textlength_lt, "100"));
        defie.add(new PairBean<>(InferrableExpression.textlength_lt, "200"));
        defie.add(new PairBean<>(InferrableExpression.is_blank, ""));
        return defie;
    }

    public Set<String> getSelectedAttributes() {
        return selectedAttributes;
    }

    public void setSelectedAttributes(Set<String> selectedAttributes) {
        if (selectedAttributes.size()==0){
            this.selectedAttributes.addAll(useMinimalAttributes());
        }else {
            this.selectedAttributes = selectedAttributes; //responsibility of the test manager
        }
    }
    public void setSelectedStateAttributes(Set<String> selectedAttributes) {
        if (selectedAttributes.size()==0){
            this.selectedAttributes.addAll(useMinimalAttributes());
        }else {
            this.selectedAttributes = selectedAttributes; //responsibility of the test manager
            this.selectedAttributes.add(TagBean.IsTerminalState.getName());
        }
    }

    public String getTagFromAttribute(String attrib){
        String ret=null;
        for (String tag : selectedAttributes  // consider to pass if selected atributes = empty?
        ) {
            if ( tag.equals(attrib)){
                ret=tag;
                break;
            }
        }
        return  ret;
    }

/*    public boolean containsTag(String attrib){
        return getTagFromAttribute(attrib) != null;
    }*/

    public Set<PairBean<InferrableExpression, String>> getSelectedExpressions() {
        return selectedExpressions;
    }

    public void setSelectedExpressions(Set<PairBean<InferrableExpression, String>> selectedExpressions) {
        if (selectedExpressions.size()==0){
            this.selectedExpressions.addAll(useMinimalSelectedExpressions());
        }else{
            this.selectedExpressions = selectedExpressions; //responsibility of the test manager
        }
    }

    //custom

    public Set<String> getAPsOfAttribute(String widgetkey, String attrib, String value) {
        Set<String> apset = new LinkedHashSet<>();
        String attributeTagName = getTagFromAttribute(attrib);


        if (attributeTagName != null) {   //if this attribute is not available in the model then no AP can be calculated.
            //lookup type
            TagBean<?> tag=null;  //refactor candidate
            for (TagBean<?> tg: getAllAttributeTags()
                 ) {
                if(tg.getName().equals(attributeTagName)){
                    tag=tg;
                    break;
                }
            }

            if (tag != null) {
                if (tag.type() == Boolean.class) {
                    if (Boolean.parseBoolean(value)) {
                        apset.add(widgetkey + attrib + "__");//encode only TRUE for genuine booleans
                    }
                } else {
                    for (PairBean<InferrableExpression, String> iap : getSelectedExpressions()
                        // just encode the TRUE/existence  and FALSE is then considered absence
                    ) {
                        if (iap.left().typ.equals("number") && (tag.type() == Double.class || tag.type() == Long.class || tag.type() == Integer.class)) {
                            int intVal = (int) Double.parseDouble(value);
                            if (((iap.left() == InferrableExpression.value_eq) && (intVal == Integer.parseInt(iap.right()))) ||
                                    ((iap.left() == InferrableExpression.value_lt) && (intVal < Integer.parseInt(iap.right())))) {
                                apset.add(widgetkey + attrib + "_" + iap.left().name()  + "_"+ iap.right() );
                            }
                        }
                        if (iap.left().typ.equals("text")) {

                            if (((iap.left() == InferrableExpression.textlength_eq) && (value.length() == Integer.parseInt(iap.right()))) ||
                                    ((iap.left() == InferrableExpression.textlength_lt) && (value.length() < Integer.parseInt(iap.right())))) {
                                apset.add(widgetkey + attrib + "_" + iap.left().name() + "_" + iap.right());
                            }

                            if (((iap.left() == InferrableExpression.textmatch))) {
                                Pattern pat = CachedRegexPatterns.addAndGet(iap.right());
                                Matcher m = pat.matcher(value);
                                if (m.matches()) {
                                    apset.add(widgetkey + attrib + "_" + iap.left().name()  + "_"+ iap.right() );
                                }
                            }

                        }
                        if (iap.left().typ.equals("shape") && (tag.type() == Shape.class)) {
                            //format:   <data key="Shape">Rect [x:459.0 y:243.0 w:116.0 h:18.0]</data>
                            //String[] shapecomponents=value.split("w:")[1];
                            String test = value.split("w:")[1].split(" ")[0];
                            int width = (int) Double.parseDouble(test);
                            test = value.split("h:")[1].split("]")[0];
                            int heigth = (int) Double.parseDouble(test);

                            if (((iap.left() == InferrableExpression.width_lt) && (width < Integer.parseInt(iap.right()))) ||
                                    ((iap.left() == InferrableExpression.heigth_lt) && (heigth < Integer.parseInt(iap.right())))
                            ) {
                                apset.add(widgetkey + attrib + "_" + iap.left().name() + "_" + iap.right() );
                            }
                        }

                        if (iap.left().typ.equals("boolean")) {   //add these regardless of the tag-type
                            //format:     <data key="Abc"></data>
                            if ((iap.left() == InferrableExpression.is_blank) && value.equals("")) {
                                apset.add(widgetkey + attrib + "_" + iap.left().name() );
                            }
                            if ((iap.left() == InferrableExpression.exists)) {
                                apset.add(widgetkey + attrib + "_" + iap.left().name() );
                            }

                        }
                    }
                }
            }
        }
        return apset;
    }
}
