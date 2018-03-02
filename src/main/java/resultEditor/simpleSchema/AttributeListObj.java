
package resultEditor.simpleSchema;

import javax.swing.Icon;
import relationship.simple.dataTypes.AttributeSchemaDef;

/**
 *
 * @author leng
 */
public class AttributeListObj{

    private AttributeSchemaDef attribute;
    private String attributename;
    private Icon icon;
    private boolean isPublicAttribute = false;

    public AttributeListObj(){
    }

    public AttributeListObj(String attributename, AttributeSchemaDef attribute, Icon icon, boolean isPublicAttribute) {
        this.attributename = attributename;
        this.icon = icon;
        this.isPublicAttribute = isPublicAttribute;
        this.attribute = attribute;

    }
    
    
    /**Indicator: is this attribute designed for UMLS codes? */
    public boolean isCode(){
        return this.attribute.isCode;
    }
    
    public void setIsCode(boolean is){
        this.attribute.isCode = is;
    }

    public String getAttributeName() {
        return attributename;
    }

    public AttributeSchemaDef getAttribute(){
        return attribute;
    }

    public void setPublic(boolean isPublicAttribute){
        this.isPublicAttribute = isPublicAttribute;
    }

    public boolean isPublicAttribute(){
        return this.isPublicAttribute;
    }

    public Icon getIcon() {
        return this.icon;
    }

    public void setAttributeName(String attributename) {
        this.attributename = attributename;
    }


    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public boolean isSelected = false;
}

