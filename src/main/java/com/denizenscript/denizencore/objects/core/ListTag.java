package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.TagRunnable;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.core.ProcedureScriptContainer;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.NaturalOrderComparator;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.core.EscapeTagBase;

import java.util.*;
import java.util.regex.Pattern;

public class ListTag extends ArrayList<String> implements ObjectTag, ObjectTag.ObjectAttributable {

    // <--[language]
    // @name ListTag
    // @group Object System
    // @description
    // A ListTag is a list of any data. It can hold any number of objects in any order.
    // The objects can be of any basic Denizen object type, including another list
    // (escaping sub-lists is strongly recommended).
    //
    // For format info, see <@link language li@>
    //
    // -->

    // <--[language]
    // @name li@
    // @group Object Fetcher System
    // @description
    // li@ refers to the 'object identifier' of a ListTag. The 'li@' is notation for Denizen's Object
    // Fetcher. The constructor for a ListTag is the content items of a list separated by a pipe '|' symbol.
    // For example, if I had a list of 'taco', 'potatoes', and 'cheese', I would use
    // 'li@taco|potatoes|cheese'
    //
    // A list with zero items in it is simply 'li@'
    // and a list with one item is just the one item and no pipes.
    //
    // For general info, see <@link language ListTag>
    // -->

    public final ArrayList<ObjectTag> objectForms;

    @Override
    public boolean add(String addMe) {
        objectForms.add(new ElementTag(addMe));
        return super.add(addMe);
    }

    @Override
    public void add(int index, String addMe) {
        objectForms.add(index, new ElementTag(addMe));
        super.add(index, addMe);
    }

    @Override
    public boolean addAll(Collection<? extends String> addMe) {
        for (String str : addMe) {
            add(str);
        }
        return !addMe.isEmpty();
    }

    @Override
    public String remove(int index) {
        objectForms.remove(index);
        return super.remove(index);
    }

    @Override
    public boolean remove(Object key) {
        int ind = super.indexOf(key);
        if (ind < 0 || ind >= size()) {
            return false;
        }
        this.remove(ind);
        return true;
    }

    public boolean addAll(ListTag inp) {
        objectForms.addAll(inp.objectForms);
        return super.addAll(inp);
    }

    public boolean addObject(ObjectTag obj) {
        objectForms.add(obj);
        return super.add(obj.toString());
    }

    public void addObject(int index, ObjectTag obj) {
        objectForms.add(index, obj);
        super.add(index, obj.toString());
    }

    public void setObject(int index, ObjectTag obj) {
        objectForms.set(index, obj);
        super.set(index, obj.toString());
    }

    public ObjectTag getObject(int id) {
        return objectForms.get(id);
    }

    public final static char internal_escape_char = (char) 0x05;
    public final static String internal_escape = String.valueOf(internal_escape_char);

    public static ListTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("li, fl")
    public static ListTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }

        ///////
        // Match @object format

        ListTag list = DenizenCore.getImplementation().valueOfFlagListTag(string);

        if (list != null) {
            return list;
        }

        // Use value of string, which will separate values by the use of a pipe '|'
        return new ListTag(string.startsWith("li@") ? string.substring(3) : string);
    }

    public static ListTag getListFor(ObjectTag inp) {
        return inp instanceof ListTag ? (ListTag) inp : valueOf(inp.toString());
    }


    public static boolean matches(String arg) {

        boolean flag = DenizenCore.getImplementation().matchesFlagListTag(arg);

        return flag || arg.contains("|") || arg.contains(internal_escape) || arg.startsWith("li@");
    }


    /////////////
    //   Constructors
    //////////

    public ListTag(Collection<? extends ObjectTag> objectTagList) {
        objectForms = new ArrayList<>(objectTagList);
        for (ObjectTag obj : objectTagList) {
            super.add(obj.identify());
        }
    }

    public ListTag() {
        objectForms = new ArrayList<>();
    }

    // A string of items, split by '|'
    public ListTag(String items) {
        if (items != null && items.length() > 0) {
            // Count brackets
            int brackets = 0;
            // Record start position
            int start = 0;
            // Loop through characters
            for (int i = 0; i < items.length(); i++) {
                char chr = items.charAt(i);
                // Count brackets
                if (chr == '[') {
                    brackets++;
                }
                else if (chr == ']') {
                    if (brackets > 0) {
                        brackets--;
                    }
                }
                // Separate if an un-bracketed pipe is found
                else if ((brackets == 0) && (chr == '|' || chr == internal_escape_char)) {
                    super.add(items.substring(start, i));
                    start = i + 1;
                }
            }
            // If there is an item waiting, add it too
            if (start < items.length()) {
                super.add(items.substring(start));
            }
        }
        objectForms = new ArrayList<>(size());
        for (String str : this) {
            objectForms.add(new ElementTag(str));
        }
    }

    public ListTag(String flag, boolean is_flag, List<String> flag_contents) {
        if (is_flag) {
            this.flag = flag;
        }
        for (String it : flag_contents) {
            super.add(it);
        }
        objectForms = new ArrayList<>(size());
        for (String str : this) {
            objectForms.add(new ElementTag(str));
        }
    }

    public ListTag(ListTag input) {
        objectForms = new ArrayList<>(input.objectForms);
        super.ensureCapacity(input.size());
        for (String str : input) {
            super.add(str);
        }
    }

    // A List<String> of items
    public ListTag(List<String> items) {
        if (items != null) {
            for (String it : items) {
                super.add(it);
            }
        }
        objectForms = new ArrayList<>(size());
        for (String str : this) {
            objectForms.add(new ElementTag(str));
        }
    }

    // A Set<Object> of items
    public ListTag(Set<?> items) {
        objectForms = new ArrayList<>();
        if (items != null) {
            for (Object o : items) {
                String strd = o.toString();
                super.add(strd);
                if (o instanceof ObjectTag) {
                    objectForms.add((ObjectTag) o);
                }
                else {
                    objectForms.add(new ElementTag(strd));
                }
            }
        }
    }

    // A List<String> of items, with a prefix
    public ListTag(List<String> items, String prefix) {
        for (String element : items) {
            super.add(prefix + element);
        }
        objectForms = new ArrayList<>(size());
        for (String str : this) {
            objectForms.add(new ElementTag(str));
        }
    }

    /////////////
    //   Instance Fields/Methods
    //////////

    public ListTag addObjects(List<ObjectTag> ObjectTags) {
        for (ObjectTag obj : ObjectTags) {
            addObject(obj);
        }

        return this;
    }

    /**
     * Fetches a String Array copy of the ListTag,
     * with the same size as the ListTag.
     *
     * @return the array copy
     */
    public String[] toArray() {
        return toArray(size());
    }

    /**
     * Fetches a String Array copy of the ListTag.
     *
     * @param arraySize the size of the new array
     * @return the array copy
     */
    public String[] toArray(int arraySize) { // TODO: Why does this exist?
        List<String> list = new ArrayList<>();

        for (String string : this) {
            list.add(string); // TODO: Why is this a manual copy?
        }

        return list.toArray(new String[arraySize]);
    }


    // Returns if the list contains objects from the specified dClass
    // by using the matches() method.
    public boolean containsObjectsFrom(Class<? extends ObjectTag> dClass) {

        // Iterate through elements until one matches() the dClass
        for (ObjectTag testable : objectForms) {
            if (CoreUtilities.canPossiblyBeType(testable, dClass)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Return a new list that includes only strings that match the values of an Enum array
     *
     * @param values the Enum's value
     * @return a filtered list
     */
    public List<String> filter(Enum[] values) {
        List<String> list = new ArrayList<>();

        for (String string : this) {
            for (Enum value : values) {
                if (value.name().equalsIgnoreCase(string)) {
                    list.add(string);
                }
            }
        }

        if (!list.isEmpty()) {
            return list;
        }
        else {
            return null;
        }
    }


    // Return a list that includes only elements belonging to a certain class
    public <T extends ObjectTag> List<T> filter(Class<T> dClass) {
        return filter(dClass, DenizenCore.getImplementation().getTagContext(null));
    }


    public <T extends ObjectTag> List<T> filter(Class<T> dClass, ScriptEntry entry) {
        return filter(dClass, (entry == null ? DenizenCore.getImplementation().getTagContext(null) :
                entry.entryData.getTagContext()));
    }

    public <T extends ObjectTag> List<T> filter(Class<T> dClass, Debuggable debugger) {
        TagContext context = DenizenCore.getImplementation().getTagContext(null);
        context.debug = debugger.shouldDebug();
        return filter(dClass, context);
    }

    public <T extends ObjectTag> List<T> filter(Class<T> dClass, TagContext context) {
        List<T> results = new ArrayList<>();

        for (ObjectTag obj : objectForms) {

            try {
                if (CoreUtilities.canPossiblyBeType(obj, dClass)) {
                    T object = CoreUtilities.asType(obj, dClass, context);

                    if (object != null) {
                        results.add(object);
                    }
                }
            }
            catch (Exception e) {
                Debug.echoError(e);
            }
        }

        return results;
    }


    @Override
    public String toString() {
        return identify();
    }


    //////////////////////////////
    //    DSCRIPT ARGUMENT METHODS
    /////////////////////////


    private String prefix = "List";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public ListTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debuggable() {
        if (isEmpty()) {
            return "li@";
        }
        StringBuilder debugText = new StringBuilder();
        debugText.append("li@");
        for (ObjectTag item : objectForms) {
            debugText.append(item.debuggable()).append(" <G>|<Y> ");
        }
        return debugText.substring(0, debugText.length() - " <G>|<Y> ".length());
    }

    public String flag = null;

    @Override
    public boolean isUnique() {
        return flag != null;
    }

    @Override
    public String getObjectType() {
        return "List";
    }

    @Override
    public String identify() {
        if (flag != null) {
            if (size() == 1) {
                return DenizenCore.getImplementation().getLastEntryFromFlag(flag);
            }
            else {
                StringBuilder dScriptArg = new StringBuilder();
                for (String item : this) {
                    dScriptArg.append(item).append('|');
                }
                return dScriptArg.substring(0, dScriptArg.length() - 1);
            }
        }
        return identifyList();
    }

    public String identifyList() {
        if (isEmpty()) {
            return "li@";
        }
        StringBuilder dScriptArg = new StringBuilder();
        dScriptArg.append("li@");
        for (String item : this) {
            dScriptArg.append(item).append('|');
        }
        return dScriptArg.substring(0, dScriptArg.length() - 1);
    }


    @Override
    public String identifySimple() {
        return identify();
    }

    public static void registerTags() {
        // <--[tag]
        // @attribute <ListTag.combine>
        // @returns ListTag
        // @description
        // returns a list containing the contents of all sublists within this list.
        // -->

        registerTag("combine", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag list = (ListTag) object;
                ListTag output = new ListTag();
                for (ObjectTag obj : list.objectForms) {
                    output.addObjects(ListTag.getListFor(obj).objectForms);
                }
                return output.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.space_separated>
        // @returns ElementTag
        // @description
        // returns the list in a cleaner format, separated by spaces.
        // For example: a list of "one|two|three" will return "one two three".
        // -->

        registerTag("space_separated", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (((ListTag) object).isEmpty()) {
                    return new ElementTag("").getObjectAttribute(attribute.fulfill(1));
                }
                return new ElementTag(parseString((ListTag) object, " ")).getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("as_string", registeredObjectTags.get("space_separated"));
        registerTag("asstring", registeredObjectTags.get("space_separated"));

        // <--[tag]
        // @attribute <ListTag.separated_by[<text>]>
        // @returns ElementTag
        // @description
        // returns the list formatted, with each item separated by the defined text.
        // For example: <li@bob|jacob|mcmonkey.separated_by[ and ]> will return "bob and jacob and mcmonkey".
        // -->

        registerTag("separated_by", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag list = (ListTag) object;
                if (list.isEmpty()) {
                    return new ElementTag("").getObjectAttribute(attribute.fulfill(1));
                }
                String input = attribute.getContext(1);
                return new ElementTag(parseString(list, input)).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.comma_separated>
        // @returns ElementTag
        // @description
        // returns the list in a cleaner format, separated by commas.
        // For example: a list of "one|two|three" will return "one, two, three".
        // -->

        registerTag("comma_separated", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (((ListTag) object).isEmpty()) {
                    return new ElementTag("").getObjectAttribute(attribute.fulfill(1));
                }
                return new ElementTag(parseString((ListTag) object, ", ")).getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("ascslist", registeredObjectTags.get("comma_separated"));
        registerTag("as_cslist", registeredObjectTags.get("comma_separated"));

        // <--[tag]
        // @attribute <ListTag.unseparated>
        // @returns ElementTag
        // @description
        // returns the list in a less clean format, separated by nothing.
        // For example: a list of "one|two|three" will return "onetwothree".
        // -->

        registerTag("unseparated", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (((ListTag) object).isEmpty()) {
                    return new ElementTag("").getObjectAttribute(attribute.fulfill(1));
                }
                return new ElementTag(parseString((ListTag) object, "")).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.get_sub_items[<#>]>
        // @returns ListTag
        // @description
        // returns a list of the specified sub items in the list, as split by the
        // forward-slash character (/).
        // For example: .get_sub_items[1] on a list of "one/alpha|two/beta" will return "one|two".
        // -->

        registerTag("get_sub_items", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                int index = -1;
                if (ArgumentHelper.matchesInteger(attribute.getContext(1))) {
                    index = attribute.getIntContext(1) - 1;
                }
                attribute.fulfill(1);

                // <--[tag]
                // @attribute <ListTag.get_sub_items[<#>].split_by[<element>]>
                // @returns ListTag
                // @description
                // returns a list of the specified sub item in the list, allowing you to specify a
                // character in which to split the sub items by. WARNING: When setting your own split
                // character, make note that it is CASE SENSITIVE.
                // For example: .get_sub_items[1].split_by[-] on a list of "one-alpha|two-beta" will return "one|two".
                // -->

                String split = "/";
                if (attribute.startsWith("split_by")) {
                    if (attribute.hasContext(1) && attribute.getContext(1).length() > 0) {
                        split = attribute.getContext(1);
                    }
                    attribute.fulfill(1);
                }

                if (index < 0) {
                    return null;
                }

                ListTag sub_list = new ListTag();

                for (String item : (ListTag) object) {
                    String[] strings = item.split(Pattern.quote(split));
                    if (strings.length > index) {
                        sub_list.add(strings[index]);
                    }
                    else {
                        sub_list.add("null");
                    }
                }

                return sub_list.getObjectAttribute(attribute);
            }
        });

        // <--[tag]
        // @attribute <ListTag.map_get[<element>]>
        // @returns ElementTag
        // @description
        // Returns the element split by the / symbol's value for the matching input element.
        // TODO: Clarify
        // For example: li@one/a|two/b.map_get[one] returns a.
        // -->

        registerTag("map_get", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (((ListTag) object).isEmpty()) {
                    return new ElementTag("").getObjectAttribute(attribute.fulfill(1));
                }
                String input = attribute.getContext(1);
                attribute.fulfill(1);

                // <--[tag]
                // @attribute <ListTag.map_get[<element>].split_by[<element>]>
                // @returns ElementTag
                // @description
                // Returns the element split by the given symbol's value for the matching input element.
                // TODO: Clarify
                // For example: li@one/a|two/b.map_get[one].split_by[/] returns a.
                // -->
                String split = "/";
                if (attribute.startsWith("split_by")) {
                    if (attribute.hasContext(1) && attribute.getContext(1).length() > 0) {
                        split = attribute.getContext(1);
                    }
                    attribute.fulfill(1);
                }

                for (String item : (ListTag) object) {
                    String[] strings = item.split(Pattern.quote(split), 2);
                    if (strings.length > 1 && strings[0].equalsIgnoreCase(input)) {
                        return new ElementTag(strings[1]).getObjectAttribute(attribute);
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <ListTag.map_find_key[<element>]>
        // @returns ElementTag
        // @description
        // Returns the element split by the / symbol's value for the matching input element.
        // TODO: Clarify
        // For example: li@one/a|two/b.map_find_key[a] returns one.
        // -->

        registerTag("map_find_key", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                String input = attribute.getContext(1);
                attribute.fulfill(1);

                // <--[tag]
                // @attribute <ListTag.map_find_key[<element>].split_by[<element>]>
                // @returns ElementTag
                // @description
                // Returns the element split by the given symbol's value for the matching input element.
                // TODO: Clarify
                // For example: li@one/a|two/b.map_find_key[a].split_by[/] returns one.
                // -->

                String split = "/";
                if (attribute.startsWith("split_by")) {
                    if (attribute.hasContext(1) && attribute.getContext(1).length() > 0) {
                        split = attribute.getContext(1);
                    }
                    attribute.fulfill(1);
                }
                for (String item : (ListTag) object) {
                    String[] strings = item.split(Pattern.quote(split), 2);
                    if (strings.length > 1 && strings[1].equalsIgnoreCase(input)) {
                        return new ElementTag(strings[0]).getObjectAttribute(attribute);
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <ListTag.size>
        // @returns ElementTag(Number)
        // @description
        // returns the size of the list.
        // For example: a list of "one|two|three" will return "3".
        // -->
        registerTag("size", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((ListTag) object).size()).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.is_empty>
        // @returns ElementTag(Boolean)
        // @description
        // returns whether the list is empty.
        // For example: a list of "" returns true, while "one" returns false.
        // -->
        registerTag("is_empty", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((ListTag) object).isEmpty()).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.insert[...|...].at[<#>]>
        // @returns ListTag
        // @description
        // returns a new ListTag with the items specified inserted to the specified location.
        // For example: .insert[two|three].at[2] on a list of "one|four" will return "one|two|three|four".
        // -->

        registerTag("insert", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.insert[...] must have a value.");
                    return null;
                }
                ListTag items = getListFor(attribute.getContextObject(1));
                attribute = attribute.fulfill(1);
                if (attribute.startsWith("at") && attribute.hasContext(1)) {
                    ListTag result = new ListTag((ListTag) object);
                    int index = new ElementTag(attribute.getContext(1)).asInt() - 1;
                    if (index < 0) {
                        index = 0;
                    }
                    if (index > result.size()) {
                        index = result.size();
                    }
                    for (int i = 0; i < items.size(); i++) {
                        result.addObject(index + i, items.getObject(i));
                    }
                    return result.getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    Debug.echoError("The tag ListTag.insert[...] must be followed by .at[#]!");
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <ListTag.set[...|...].at[<#>]>
        // @returns ListTag
        // @description
        // returns a new ListTag with the items specified inserted to the specified location, replacing the element
        // already at that location.
        // For example: .set[potato].at[2] on a list of "one|two|three" will return "one|potato|three".
        // -->

        registerTag("set", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.set[...] must have a value.");
                    return null;
                }
                if (((ListTag) object).isEmpty()) {
                    return null;
                }
                ListTag items = getListFor(attribute.getContextObject(1));
                attribute = attribute.fulfill(1);
                if (attribute.startsWith("at")
                        && attribute.hasContext(1)) {
                    ListTag result = new ListTag((ListTag) object);
                    int index = ArgumentHelper.getIntegerFrom(attribute.getContext(1)) - 1;
                    if (index < 0) {
                        index = 0;
                    }
                    if (index > result.size() - 1) {
                        index = result.size() - 1;
                    }
                    result.remove(index);
                    for (int i = 0; i < items.size(); i++) {
                        result.addObject(index + i, items.objectForms.get(i));
                    }
                    return result.getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    Debug.echoError("The tag ListTag.set[...] must be followed by .at[#]!");
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <ListTag.include[...|...]>
        // @returns ListTag
        // @description
        // returns a new ListTag including the items specified.
        // For example: .include[three|four] on a list of "one|two" will return "one|two|three|four".
        // -->

        registerTag("include", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.include[...] must have a value.");
                    return null;
                }
                ListTag copy = new ListTag((ListTag) object);
                copy.addAll(getListFor(attribute.getContextObject(1)));
                return copy.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.exclude[...|...]>
        // @returns ListTag
        // @description
        // returns a new ListTag excluding the items specified.
        // For example: .exclude[two|four] on a list of "one|two|three|four" will return "one|three".
        // -->

        registerTag("exclude", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.exclude[...] must have a value.");
                    return null;
                }
                ListTag exclusions = getListFor(attribute.getContextObject(1));
                // Create a new ListTag that will contain the exclusions
                ListTag copy = new ListTag((ListTag) object);
                // Iterate through
                for (String exclusion : exclusions) {
                    for (int i = 0; i < copy.size(); i++) {
                        if (copy.get(i).equalsIgnoreCase(exclusion)) {
                            copy.remove(i--);
                        }
                    }
                }
                // Return the modified list
                return copy.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.remove[<#>|...]>
        // @returns ListTag
        // @description
        // returns a new ListTag excluding the items at the specified index.
        // For example: .remove[2] on a list of "one|two|three|four" will return "one|three|four".
        // Also supports [first] and [last] values.
        // -->

        registerTag("remove", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.remove[#] must have a value.");
                    return null;
                }
                ListTag indices = getListFor(attribute.getContextObject(1));
                ListTag copy = new ListTag((ListTag) object);
                for (String index : indices) {
                    int remove;
                    if (index.equalsIgnoreCase("last")) {
                        remove = copy.size() - 1;
                    }
                    else if (index.equalsIgnoreCase("first")) {
                        remove = 0;
                    }
                    else {
                        remove = new ElementTag(index).asInt() - 1;
                    }
                    if (remove >= 0 && remove < copy.size()) {
                        copy.set(remove, "\0");
                    }
                }
                for (int i = 0; i < copy.size(); i++) {
                    if (copy.get(i).equals("\0")) {
                        copy.remove(i--);
                    }
                }
                return copy.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.replace[(regex:)<element>]>
        // @returns ElementTag
        // @description
        // Returns the list with all instances of an element removed.
        // Specify regex: at the start of the replace element to replace elements that match the Regex.
        // -->

        // <--[tag]
        // @attribute <ListTag.replace[(regex:)<element>].with[<element>]>
        // @returns ListTag
        // @description
        // Returns the list with all instances of an element replaced with another.
        // Specify regex: at the start of the replace element to replace elements that match the Regex.
        // -->
        registerTag("replace", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.replace[...] must have a value.");
                    return null;
                }
                String replace = attribute.getContext(1);
                ObjectTag replacement = null;
                attribute.fulfill(1);
                if (attribute.startsWith("with")) {
                    if (attribute.hasContext(1)) {
                        replacement = attribute.getContextObject(1);
                        attribute.fulfill(1);
                    }
                }

                ListTag obj = (ListTag) object;
                ListTag list = new ListTag();

                if (replace.startsWith("regex:")) {
                    String regex = replace.substring("regex:".length());
                    Pattern tempPat = Pattern.compile(regex);
                    for (int i = 0; i < obj.size(); i++) {
                        if (tempPat.matcher(obj.get(i)).matches()) {
                            if (replacement != null) {
                                list.addObject(replacement);
                            }
                        }
                        else {
                            list.addObject(obj.getObject(i));
                        }
                    }
                }
                else {
                    String lower = CoreUtilities.toLowerCase(replace);
                    for (int i = 0; i < obj.size(); i++) {
                        if (CoreUtilities.toLowerCase(obj.get(i)).equals(lower)) {
                            if (replacement != null) {
                                list.addObject(replacement);
                            }
                        }
                        else {
                            list.addObject(obj.getObject(i));
                        }
                    }
                }

                return list.getObjectAttribute(attribute);
            }
        });

        // <--[tag]
        // @attribute <ListTag.reverse>
        // @returns ListTag
        // @description
        // returns a copy of the list, with all items placed in opposite order.
        // For example: a list of "one|two|three" will become "three|two|one".
        // -->

        registerTag("reverse", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ArrayList<ObjectTag> objs = new ArrayList<>(((ListTag) object).objectForms);
                Collections.reverse(objs);
                return new ListTag(objs).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.deduplicate>
        // @returns ListTag
        // @description
        // returns a copy of the list with any duplicate items removed.
        // For example: a list of "one|one|two|three" will become "one|two|three".
        // -->

        registerTag("deduplicate", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag obj = (ListTag) object;
                ListTag list = new ListTag();
                int size = obj.size();
                for (int i = 0; i < size; i++) {
                    String entry = obj.get(i);
                    boolean duplicate = false;
                    for (int x = 0; x < i; x++) {
                        if (obj.get(x).equalsIgnoreCase(entry)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        list.addObject(obj.objectForms.get(i));
                    }
                }
                return list.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.get[<#>|...]>
        // @returns ObjectTag
        // @description
        // returns an element of the value specified by the supplied context.
        // For example: .get[1] on a list of "one|two" will return "one", and .get[2] will return "two"
        // Specify more than one index to get a list of results.
        // -->
        TagRunnable.ObjectForm getRunnable = new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.get[...] must have a value.");
                    return null;
                }
                ListTag list = (ListTag) object;
                if (list.isEmpty()) {
                    if (!attribute.hasAlternative()) {
                        Debug.echoError("Can't get from an empty list.");
                    }
                    return null;
                }
                ListTag indices = getListFor(attribute.getContextObject(1));
                if (indices.size() > 1) {
                    ListTag results = new ListTag();
                    for (String index : indices) {
                        int ind = ArgumentHelper.getIntegerFrom(index);
                        if (ind > 0 && ind <= list.size()) {
                            results.add(list.get(ind - 1));
                        }
                    }
                    return results.getObjectAttribute(attribute.fulfill(1));
                }
                if (indices.size() > 0) {
                    int index = ArgumentHelper.getIntegerFrom(indices.get(0)) - 1;
                    if (index >= list.size()) {
                        if (!attribute.hasAlternative()) {
                            Debug.echoError("Invalid list.get index.");
                        }
                        return null;
                    }
                    if (index < 0) {
                        index = 0;
                    }
                    attribute = attribute.fulfill(1);

                    // <--[tag]
                    // @attribute <ListTag.get[<#>].to[<#>]>
                    // @returns ListTag
                    // @description
                    // returns all elements in the range from the first index to the second.
                    // For example: .get[1].to[3] on a list of "one|two|three|four" will return "one|two|three"
                    // -->
                    if (attribute.startsWith("to") && attribute.hasContext(1)) {
                        int index2 = attribute.getIntContext(1) - 1;
                        if (index2 >= list.size()) {
                            index2 = list.size() - 1;
                        }
                        if (index2 < 0) {
                            index2 = 0;
                        }
                        ListTag newList = new ListTag();
                        for (int i = index; i <= index2; i++) {
                            newList.addObject(list.objectForms.get(i));
                        }
                        return newList.getObjectAttribute(attribute.fulfill(1));
                    }
                    else {
                        return CoreUtilities.autoAttribTyped(list.objectForms.get(index), attribute);
                    }
                }
                return null;
            }
        };
        registerTag("get", getRunnable.clone());
        registerTag("", getRunnable.clone());

        // <--[tag]
        // @attribute <ListTag.find_all_partial[<element>]>
        // @returns ListTag(Element(Number))
        // @description
        // returns all the numbered locations of elements that contain the text within a list,
        // or an empty list if the list does not contain that item.
        // For example: .find_all_partial[tw] on a list of "one|two|three|two" will return "2|4".
        // TODO: Take multiple inputs? Or a regex?
        // -->

        registerTag("find_all_partial", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.find_all_partial[...] must have a value.");
                    return null;
                }
                ListTag list = (ListTag) object;
                String test = attribute.getContext(1).toUpperCase();
                ListTag positions = new ListTag();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).toUpperCase().contains(test)) {// TODO: Efficiency
                        positions.add(String.valueOf(i + 1));
                    }
                }
                return positions.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.find_all[<element>]>
        // @returns ListTag(Element(Number))
        // @description
        // returns all the numbered locations of elements that match the text within a list,
        // or an empty list if the list does not contain that item.
        // For example: .find_all[two] on a list of "one|two|three|two" will return "2|4".
        // TODO: Take multiple inputs? Or a regex?
        // -->

        registerTag("find_all", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.find_all[...] must have a value.");
                    return null;
                }
                ListTag list = (ListTag) object;
                ListTag positions = new ListTag();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).equalsIgnoreCase(attribute.getContext(1))) {
                        positions.add(String.valueOf(i + 1));
                    }
                }
                return positions.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.find_partial[<element>]>
        // @returns ElementTag(Number)
        // @description
        // returns the numbered location of the first partially matching element within a list,
        // or -1 if the list does not contain that item.
        // For example: .find_partial[tw] on a list of "one|two|three" will return "2".
        // TODO: Take multiple inputs? Or a regex?
        // -->

        registerTag("find_partial", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.find_partial[...] must have a value.");
                    return null;
                }
                ListTag list = (ListTag) object;
                String test = attribute.getContext(1).toUpperCase();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).toUpperCase().contains(test)) { // TODO: Efficiency
                        return new ElementTag(i + 1).getObjectAttribute(attribute.fulfill(1));
                    }
                }
                return new ElementTag(-1).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.find[<element>]>
        // @returns ElementTag(Number)
        // @description
        // returns the numbered location of an element within a list,
        // or -1 if the list does not contain that item.
        // For example: .find[two] on a list of "one|two|three" will return "2".
        // TODO: Take multiple inputs? Or a regex?
        // -->

        registerTag("find", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.find[...] must have a value.");
                    return null;
                }
                ListTag list = (ListTag) object;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).equalsIgnoreCase(attribute.getContext(1))) {
                        return new ElementTag(i + 1).getObjectAttribute(attribute.fulfill(1));
                    }
                }
                // TODO: This should be find_partial or something
            /*
            for (int i = 0; i < size(); i++) {
                if (get(i).toUpperCase().contains(attribute.getContext(1).toUpperCase()))
                    return new Element(i + 1).getObjectAttribute(attribute.fulfill(1));
            }
            */
                return new ElementTag(-1).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.count[<element>]>
        // @returns ElementTag(Number)
        // @description
        // returns how many times in the sub-list occurs.
        // For example: a list of "one|two|two|three" .count[two] returns 2.
        // -->

        registerTag("count", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.count[...] must have a value.");
                    return null;
                }
                ListTag list = (ListTag) object;
                String element = attribute.getContext(1);
                int count = 0;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).equalsIgnoreCase(element)) {
                        count++;
                    }
                }
                return new ElementTag(count).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.sum>
        // @returns ElementTag(Number)
        // @description
        // returns the sum of all numbers in the list.
        // -->

        registerTag("sum", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag list = (ListTag) object;
                double sum = 0;
                for (String entry : list) {
                    sum += ArgumentHelper.getDoubleFrom(entry);
                }
                return new ElementTag(sum).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.average>
        // @returns ElementTag(Number)
        // @description
        // returns the average of all numbers in the list.
        // -->

        registerTag("average", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag list = (ListTag) object;
                if (list.isEmpty()) {
                    return new ElementTag(0).getObjectAttribute(attribute.fulfill(1));
                }
                double sum = 0;
                for (String entry : list) {
                    sum += ArgumentHelper.getDoubleFrom(entry);
                }
                return new ElementTag(sum / list.size()).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.first>
        // @returns ObjectTag
        // @description
        // returns the first element in the list.
        // If the list is empty, returns null instead.
        // For example: a list of "one|two|three" will return "one".
        // Effectively equivalent to .get[1]
        // -->

        registerTag("first", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag list = (ListTag) object;
                if (list.isEmpty()) {
                    return null;
                }
                else {
                    return CoreUtilities.autoAttribTyped(list.objectForms.get(0), attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <ListTag.last>
        // @returns ObjectTag
        // @description
        // returns the last element in the list.
        // If the list is empty, returns null instead.
        // For example: a list of "one|two|three" will return "three".
        // Effectively equivalent to .get[<list.size>]
        // -->

        registerTag("last", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag list = (ListTag) object;
                if (list.isEmpty()) {
                    return null;
                }
                else {
                    return CoreUtilities.autoAttribTyped(list.objectForms.get(list.size() - 1), attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <ListTag.numerical>
        // @returns ListTag
        // @description
        // returns the list sorted to be in numerical order.
        // For example: a list of "3|2|1|10" will return "1|2|3|10".
        // -->

        registerTag("numerical", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ArrayList<String> sortable = new ArrayList<>((ListTag) object);
                Collections.sort(sortable, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        double value = new ElementTag(o1).asDouble() - new ElementTag(o2).asDouble();
                        if (value == 0) {
                            return 0;
                        }
                        else if (value > 0) {
                            return 1;
                        }
                        else {
                            return -1;
                        }
                    }
                });
                return new ListTag(sortable).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.alphanumeric>
        // @returns ElementTag
        // @description
        // returns the list sorted to be in alphabetical/numerical order.
        // For example: a list of "b|c|a10|a1" will return "a1|a10|b|c".
        // -->

        registerTag("alphanumeric", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ArrayList<String> sortable = new ArrayList<>((ListTag) object);
                Collections.sort(sortable, new NaturalOrderComparator());
                return new ListTag(sortable).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.alphabetical>
        // @returns ElementTag
        // @description
        // returns the list sorted to be in alphabetical order.
        // For example: a list of "c|d|q|a|g" will return "a|c|d|g|q".
        // -->

        registerTag("alphabetical", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ArrayList<String> sortable = new ArrayList<>((ListTag) object);
                Collections.sort(sortable, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.compareToIgnoreCase(o2);
                    }
                });
                return new ListTag(sortable).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.sort_by_number[<tag>]>
        // @returns ListTag
        // @description
        // returns a copy of the list, sorted such that the lower numbers appear first, and the higher numbers appear last.
        // Rather than sorting based on the item itself, it sorts based on a tag attribute read from within the object being read.
        // For example, you might sort a list of players based on the amount of money they have, via .sort_by_number[money] on the list of valid players.
        // -->

        registerTag("sort_by_number", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(final Attribute attribute, final ObjectTag object) {
                ListTag newlist = new ListTag((ListTag) object);
                try {
                    Collections.sort(newlist.objectForms, new Comparator<ObjectTag>() {
                        @Override
                        public int compare(ObjectTag o1, ObjectTag o2) {
                            ObjectTag or1 = CoreUtilities.autoAttribTyped(o1, new Attribute(attribute.getContext(1), attribute.getScriptEntry(), attribute.context));
                            ObjectTag or2 = CoreUtilities.autoAttribTyped(o2, new Attribute(attribute.getContext(1), attribute.getScriptEntry(), attribute.context));
                            double r1 = ArgumentHelper.getDoubleFrom(or1.toString());
                            double r2 = ArgumentHelper.getDoubleFrom(or2.toString());
                            double value = r1 - r2;
                            if (value == 0) {
                                return 0;
                            }
                            else if (value > 0) {
                                return 1;
                            }
                            else {
                                return -1;
                            }
                        }
                    });
                    return new ListTag(newlist.objectForms).getObjectAttribute(attribute.fulfill(1));
                }
                catch (Exception ex) {
                    Debug.echoError(ex);
                }
                return newlist.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.sort[<procedure>]>
        // @returns ListTag
        // @description
        // returns a list sorted according to the return values of a procedure.
        // The <procedure> should link a procedure script that takes two definitions each of which will be an item
        // in the list, and returns -1, 0, or 1 based on whether the second item should be added. EG, if a procedure
        // with definitions "one" and "two" returned -1, it would place "two" after "one". Note that this
        // uses some complex internal sorting code that could potentially throw errors if the procedure does not return
        // consistently - EG, if "one" and "two" returned 1, but "two" and "one" returned 1 as well - obviously,
        // "two" can not be both before AND after "one"!
        // Note that the script should ALWAYS return -1, 0, or 1, or glitches could happen!
        // Note that if two inputs are exactly equal, the procedure should always return 0.
        // -->

        registerTag("sort", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag obj = new ListTag((ListTag) object);
                final ProcedureScriptContainer script = (ProcedureScriptContainer) ScriptTag.valueOf(attribute.getContext(1)).getContainer();
                if (script == null) {
                    Debug.echoError("'" + attribute.getContext(1) + "' is not a valid procedure script!");
                    return obj.getObjectAttribute(attribute.fulfill(1));
                }
                final ScriptEntry entry = attribute.getScriptEntry();
                attribute = attribute.fulfill(1);
                // <--[tag]
                // @attribute <ListTag.sort[<procedure>].context[<context>]>
                // @returns ElementTag
                // @description
                // Sort a list, with context. See <@link tag ListTag.sort[<procedure>]> for general sort details.
                // -->
                ListTag context = new ListTag();
                if (attribute.startsWith("context")) {
                    context = getListFor(attribute.getContextObject(1));
                    attribute = attribute.fulfill(1);
                }
                final ListTag context_send = context;
                List<String> list = new ArrayList<>(obj);
                try {
                    Collections.sort(list, new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            List<ScriptEntry> entries = script.getBaseEntries(entry == null ?
                                    DenizenCore.getImplementation().getEmptyScriptEntryData() : entry.entryData.clone());
                            if (entries.isEmpty()) {
                                return 0;
                            }
                            InstantQueue queue = new InstantQueue("LISTTAG_SORT");
                            queue.addEntries(entries);
                            int x = 1;
                            ListTag definitions = new ListTag();
                            definitions.add(o1);
                            definitions.add(o2);
                            definitions.addAll(context_send);
                            String[] definition_names = null;
                            try {
                                definition_names = script.getString("definitions").split("\\|");
                            }
                            catch (Exception e) { /* IGNORE */ }
                            for (String definition : definitions) {
                                String name = definition_names != null && definition_names.length >= x ?
                                        definition_names[x - 1].trim() : String.valueOf(x);
                                queue.addDefinition(name, definition);
                                Debug.echoDebug(entries.get(0), "Adding definition %" + name + "% as " + definition);
                                x++;
                            }
                            queue.start();
                            int res = 0;
                            if (queue.determinations != null && queue.determinations.size() > 0) {
                                res = new ElementTag(queue.determinations.get(0)).asInt();
                            }
                            if (res < 0) {
                                return -1;
                            }
                            else if (res > 0) {
                                return 1;
                            }
                            else {
                                return 0;
                            }
                        }
                    });
                }
                catch (Exception e) {
                    Debug.echoError("list.sort[...] tag failed - procedure returned unreasonable response - internal error: " + e.getMessage());
                }
                return new ListTag(list).getObjectAttribute(attribute);
            }
        });

        // <--[tag]
        // @attribute <ListTag.filter[<tag>]>
        // @returns ListTag
        // @description
        // returns a copy of the list with all its contents parsed through the given tag and only including ones that returned 'true'.
        // For example: a list of '1|2|3|4|5' .filter[is[or_more].than[3]] returns a list of '3|4|5'.
        // -->

        registerTag("filter", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                String tag = attribute.getContext(1);
                boolean defaultValue = tag.endsWith("||true");
                if (defaultValue) {
                    tag = tag.substring(0, tag.length() - "||true".length());
                }
                ListTag newlist = new ListTag();
                try {
                    for (ObjectTag obj : ((ListTag) object).objectForms) {
                        Attribute tempAttrib = new Attribute(tag,
                                attribute.getScriptEntry(), attribute.context);
                        tempAttrib.setHadAlternative(true);
                        ObjectTag objs = CoreUtilities.autoAttribTyped(obj, tempAttrib);
                        if ((objs == null) ? defaultValue : CoreUtilities.toLowerCase(objs.toString()).equals("true")) {
                            newlist.addObject(obj);
                        }
                    }
                }
                catch (Exception ex) {
                    Debug.echoError(ex);
                }
                return newlist.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.parse[<tag>]>
        // @returns ListTag
        // @description
        // returns a copy of the list with all its contents parsed through the given tag.
        // For example: a list of 'one|two' .parse[to_uppercase] returns a list of 'ONE|TWO'.
        // -->

        registerTag("parse", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag newlist = new ListTag();
                String tag = attribute.getContext(1);
                String defaultValue = "null";
                boolean fallback = false;
                if (tag.contains("||")) {
                    int marks = 0;
                    int lengthLimit = tag.length() - 1;
                    for (int i = 0; i < lengthLimit; i++) {
                        char c = tag.charAt(i);
                        if (c == '<') {
                            marks++;
                        }
                        else if (c == '>') {
                            marks--;
                        }
                        else if (marks == 0 && c == '|' && tag.charAt(i + 1) == '|') {
                            fallback = true;
                            defaultValue = tag.substring(i + 2);
                            tag = tag.substring(0, i);
                            break;
                        }
                    }
                }
                try {
                    for (ObjectTag obj : ((ListTag) object).objectForms) {
                        Attribute tempAttrib = new Attribute(tag,
                                attribute.getScriptEntry(), attribute.context);
                        tempAttrib.setHadAlternative(attribute.hasAlternative() || fallback);
                        ObjectTag objs = CoreUtilities.autoAttribTyped(obj, tempAttrib);
                        if (objs == null) {
                            objs = new ElementTag(defaultValue);
                        }
                        newlist.addObject(objs);
                    }
                }
                catch (Exception ex) {
                    Debug.echoError(ex);
                }
                return newlist.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.pad_left[<#>]>
        // @returns ListTag
        // @description
        // Returns a ListTag extended to reach a minimum specified length
        // by adding entries to the left side.
        // -->

        registerTag("pad_left", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.pad_left[...] must have a value.");
                    return null;
                }
                ObjectTag with = new ElementTag("");
                int length = attribute.getIntContext(1);
                attribute = attribute.fulfill(1);

                // <--[tag]
                // @attribute <ListTag.pad_left[<#>].with[<element>]>
                // @returns ListTag
                // @description
                // Returns a ListTag extended to reach a minimum specified length
                // by adding a specific entry to the left side.
                // -->
                if (attribute.startsWith("with")
                        && attribute.hasContext(1)) {
                    with = attribute.getContextObject(1);
                    attribute = attribute.fulfill(1);
                }

                ListTag newList = new ListTag((ListTag) object);
                while (newList.size() < length) {
                    newList.addObject(with);
                }

                return newList.getObjectAttribute(attribute);
            }
        });

        // <--[tag]
        // @attribute <ListTag.pad_right[<#>]>
        // @returns ListTag
        // @description
        // Returns a ListTag extended to reach a minimum specified length
        // by adding entries to the right side.
        // -->

        registerTag("pad_right", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.pad_right[...] must have a value.");
                    return null;
                }
                ObjectTag with = new ElementTag("");
                int length = attribute.getIntContext(1);
                attribute = attribute.fulfill(1);

                // <--[tag]
                // @attribute <ListTag.pad_right[<#>].with[<element>]>
                // @returns ListTag
                // @description
                // Returns a ListTag extended to reach a minimum specified length
                // by adding a specific entry to the right side.
                // -->
                if (attribute.startsWith("with")
                        && attribute.hasContext(1)) {
                    with = attribute.getContextObject(1);
                    attribute = attribute.fulfill(1);
                }

                ListTag newList = new ListTag((ListTag) object);
                while (newList.size() < length) {
                    newList.addObject(with);
                }

                return newList.getObjectAttribute(attribute);
            }
        });

        // <--[tag]
        // @attribute <ListTag.escape_contents>
        // @returns ListTag
        // @description
        // returns a copy of the list with all its contents escaped.
        // Inverts <@link tag ListTag.unescape_contents>.
        // See <@link language property escaping>.
        // -->

        registerTag("escape_contents", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag escaped = new ListTag();
                for (String entry : (ListTag) object) {
                    escaped.add(EscapeTagBase.escape(entry));
                }
                return escaped.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.unescape_contents>
        // @returns ListTag
        // @description
        // returns a copy of the list with all its contents unescaped.
        // Inverts <@link tag ListTag.escape_contents>.
        // See <@link language property escaping>.
        // -->

        registerTag("unescape_contents", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag escaped = new ListTag();
                for (String entry : (ListTag) object) {
                    escaped.add(EscapeTagBase.unEscape(entry));
                }
                return escaped.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.contains_any_case_sensitive[<element>|...]>
        // @returns ElementTag(Boolean)
        // @description
        // returns whether the list contains any of a list of given elements, case-sensitive.
        // -->

        registerTag("contains_any_case_sensitive", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.contains_any_case_sensitive[...] must have a value.");
                    return null;
                }
                ListTag list = getListFor(attribute.getContextObject(1));
                boolean state = false;

                full_set:
                for (String element : (ListTag) object) {
                    for (String sub_element : list) {
                        if (element.equals(sub_element)) {
                            state = true;
                            break full_set;
                        }
                    }
                }

                return new ElementTag(state).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.contains_any[<element>|...]>
        // @returns ElementTag(Boolean)
        // @description
        // returns whether the list contains any of a list of given elements.
        // -->

        registerTag("contains_any", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.contains_any[...] must have a value.");
                    return null;
                }
                ListTag list = getListFor(attribute.getContextObject(1));
                boolean state = false;

                full_set:
                for (String element : (ListTag) object) {
                    for (String sub_element : list) {
                        if (element.equalsIgnoreCase(sub_element)) {
                            state = true;
                            break full_set;
                        }
                    }
                }

                return new ElementTag(state).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.contains_case_sensitive[<element>]>
        // @returns ElementTag(Boolean)
        // @description
        // returns whether the list contains a given element, case-sensitive.
        // -->

        registerTag("contains_case_sensitive", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.contains_case_sensitive[...] must have a value.");
                    return null;
                }
                boolean state = false;

                for (String element : (ListTag) object) {
                    if (element.equals(attribute.getContext(1))) {
                        state = true;
                        break;
                    }
                }

                return new ElementTag(state).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.contains[<element>|...]>
        // @returns ElementTag(Boolean)
        // @description
        // returns whether the list contains all of the given elements.
        // -->

        registerTag("contains", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ListTag.contains[...] must have a value.");
                    return null;
                }
                ListTag needed = getListFor(attribute.getContextObject(1));
                int gotten = 0;

                for (String check : needed) {
                    for (String element : (ListTag) object) {
                        if (element.equalsIgnoreCase(check)) {
                            gotten++;
                            break;
                        }
                    }
                }

                return new ElementTag(gotten == needed.size() && gotten > 0).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.type>
        // @returns ElementTag
        // @description
        // Always returns 'List' for ListTag objects. All objects fetchable by the Object Fetcher will return the
        // type of object that is fulfilling this attribute.
        // -->

        registerTag("type", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag("List").getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <ListTag.random[<#>]>
        // @returns ObjectTag
        // @description
        // Gets a random item in the list and returns it as an Element.
        // Optionally, add [<#>] to get a list of multiple randomly chosen elements.
        // For example: .random on a list of "one|two" could return EITHER "one" or "two" - different each time!
        // For example: .random[2] on a list of "one|two|three" could return "one|two", "two|three", OR "one|three" - different each time!
        // For example: .random[9999] on a list of "one|two|three" could return "one|two|three", "one|three|two", "two|one|three",
        // "two|three|one", "three|two|one", OR "three|one|two" - different each time!
        // -->

        registerTag("random", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag obj = (ListTag) object;
                if (obj.isEmpty()) {
                    return null;
                }
                if (attribute.hasContext(1)) {
                    int count = Integer.valueOf(attribute.getContext(1));
                    int times = 0;
                    ArrayList<ObjectTag> available = new ArrayList<>();
                    available.addAll(obj.objectForms);
                    ListTag toReturn = new ListTag();
                    while (!available.isEmpty() && times < count) {
                        int random = CoreUtilities.getRandom().nextInt(available.size());
                        toReturn.addObject(available.get(random));
                        available.remove(random);
                        times++;
                    }
                    return toReturn.getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return CoreUtilities.autoAttribTyped(obj.objectForms.get(CoreUtilities.getRandom().nextInt(obj.size())),
                            attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <ListTag.closest_to[<text>]>
        // @returns ElementTag
        // @description
        // Returns the item in the list that seems closest to the given value.
        // Particularly useful for command handlers, "<li@c1|c2|c3|[...].closest_to[<argument>]>" to get the best option as  "did you mean" suggestion.
        // For example, "<li@dance|quit|spawn.closest_to[spwn]>" returns "spawn".
        // Be warned that this will always return /something/, excluding the case of an empty list, which will return an empty element.
        // Uses the logic of tag "ElementTag.difference"!
        // You can use that tag to add an upper limit on how different the strings can be.
        // -->

        registerTag("closest_to", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(CoreUtilities.getClosestOption((ListTag) object, attribute.getContext(1)))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });


        // <--[tag]
        // @attribute <ListTag.type>
        // @returns ElementTag
        // @description
        // Always returns 'List' for ListTag objects. All objects fetchable by the Object Fetcher will return the
        // type of object that is fulfilling this attribute.
        // -->

        registerTag("type", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag("List").getObjectAttribute(attribute.fulfill(1));
            }
        });

    }

    //public static HashMap<String, TagRunnable> registeredTags = new HashMap<String, TagRunnable>();

    public static HashMap<String, TagRunnable.ObjectForm> registeredObjectTags = new HashMap<>();

    public static void registerTag(String name, TagRunnable.ObjectForm runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        registeredObjectTags.put(name, runnable);
    }

    private static String parseString(ListTag obj, String spacer) {

        StringBuilder dScriptArg = new StringBuilder();
        for (String item : obj) {
            dScriptArg.append(item);
            dScriptArg.append(spacer);
        }
        return dScriptArg.toString().substring(0,
                dScriptArg.length() - spacer.length());
    }

    @Override
    public String getAttribute(Attribute attribute) {
        return CoreUtilities.stringifyNullPass(getObjectAttribute(attribute));
    }

    @Override
    public <T extends ObjectTag> T asObjectType(Class<T> type, TagContext context) {
        return null;
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {

        if (attribute == null) {
            return null;
        }

        if (attribute.isComplete()) {
            return this;
        }

        // TODO: Scrap getObjectAttribute, make this functionality a core system
        String attrLow = CoreUtilities.toLowerCase(attribute.getAttributeWithoutContext(1));
        TagRunnable.ObjectForm otr = registeredObjectTags.get(attrLow);
        if (otr != null) {
            if (!otr.name.equals(attrLow)) {
                Debug.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + otr.name + "': '" + attrLow + "'.");
            }
            return otr.run(attribute, this);
        }

        if (Debug.verbose) {
            Debug.log("ListTag alternate attribute " + attrLow);
        }
        if (ArgumentHelper.matchesInteger(attrLow)) {
            int index = ArgumentHelper.getIntegerFrom(attrLow);
            if (index != 0) {
                if (index < 1 || index > size()) {
                    if (!attribute.hasAlternative()) {
                        Debug.echoError("ListTag index " + index + " is out of range");
                    }
                    attribute.fulfill(1);
                    return null;
                }
                return getObject(index - 1).getObjectAttribute(attribute.fulfill(1));
            }
        }

        /*
        TagRunnable tr = registeredTags.get(attrLow);
        if (tr != null) {
            if (!tr.name.equals(attrLow)) {
                dB.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + tr.name + "': '" + attrLow + "'.");
            }
            return new Element(tr.run(attribute, this));
        }*/

        //
        // TODO: Everything below is deprecated and will be moved to registerTag() format
        //

        // FLAG Specific Attributes

        // Note: is_expired attribute is handled in player/npc/server
        // since expired flags return 'null'

        // <--[tag]
        // @attribute <fl@flag_name.is_expired>
        // @returns ElementTag(Boolean)
        // @description
        // returns true of the flag is expired or does not exist, false if it
        // is not yet expired, or has no expiration.
        // -->

        // Need this attribute (for flags) since they return the last
        // element of the list, unless '.as_list' is specified.

        // <--[tag]
        // @attribute <fl@flag_name.as_list>
        // @returns ListTag
        // @description
        // returns a ListTag containing the items in the flag.
        // -->
        if (flag != null && (attribute.startsWith("as_list")
                || attribute.startsWith("aslist"))) {
            return new ListTag(this).getObjectAttribute(attribute.fulfill(1));
        }


        ObjectTag returned = CoreUtilities.autoPropertyTagObject(this, attribute);
        if (returned != null) {
            return returned;
        }

        // If this is a flag, return the last element (this is how it has always worked...)
        // Use as_list to return a list representation of the flag.
        // If this is NOT a flag, but instead a normal ListTag, return an element
        // with ListTag's identify() value.

        return (flag != null
                ? new ElementTag(DenizenCore.getImplementation().getLastEntryFromFlag(flag)).getObjectAttribute(attribute)
                : new ElementTag(identifyList()).getObjectAttribute(attribute));
    }
}