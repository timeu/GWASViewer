package at.gmi.nordborglab.widgets.gwasgeneviewer.client;

import com.google.gwt.ajaxloader.client.Properties;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.DataView;

/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 3/25/13
 * Time: 3:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class CustomDataView extends DataView {

    protected CustomDataView() {}


    public final native JsArrayInteger getFilteredRows(JsArray<Properties> properties)/*-{
        return this.getFilteredRows(properties);
    }-*/;

    public static final native JsArrayInteger getFilteredRows(AbstractDataTable dataTable,JsArray<Properties> properties)/*-{
        return dataTable.getFilteredRows(properties);
    }-*/;
}
