<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- 

     This stylesheet converts the output of com.mysql.jdbc.util.PropertiesDocGenerator
     to a DocBook table for inclusion in the product manual.

-->

<xsl:template match="ConnectionProperties">
	<xsl:element name="table">
		<xsl:element name="title">Connection Properties</xsl:element>
    	<xsl:element name="tgroup"><xsl:attribute name="cols">4</xsl:attribute>
    	  	<xsl:element name="colspec">
    			<xsl:attribute name="colname">cj_propstbl_prop_name</xsl:attribute>
			</xsl:element>
           	<xsl:element name="colspec">
           		<xsl:attribute name="colname">cj_propstbl_prop_defn</xsl:attribute>
			</xsl:element>
            <xsl:element name="colspec">
            	<xsl:attribute name="colname">cj_propstbl_required</xsl:attribute>
			</xsl:element>
            <xsl:element name="colspec">
            	<xsl:attribute name="colname">cj_propstbl_required</xsl:attribute>
			</xsl:element>
            <xsl:element name="colspec">
            	<xsl:attribute name="colname">cj_propstbl_since_version</xsl:attribute>
			</xsl:element>
			
            <xsl:element name="spanspec">
            	<xsl:attribute name="nameend">cj_propstbl_since_version</xsl:attribute>
            	<xsl:attribute name="namest">cj_propstbl_prop_name</xsl:attribute>
            	<xsl:attribute name="spanname">cj_propstbl_span_all_cols</xsl:attribute>
			</xsl:element>
			
    		<xsl:element name="thead">
    			<xsl:element name="row">
    				<xsl:element name="entry">Property Name</xsl:element>
    				<xsl:element name="entry">Definition</xsl:element>
    				<xsl:element name="entry">Required?</xsl:element>
    				<xsl:element name="entry">Default Value</xsl:element>
    				<xsl:element name="entry">Since Version</xsl:element>
				</xsl:element> <!-- row -->
			</xsl:element> <!-- thead -->
		

			<xsl:element name="tbody">
			
	
				<xsl:apply-templates select = "PropertyCategory" />
			</xsl:element> <!-- tbody -->
		</xsl:element> <!-- tgroup -->
	</xsl:element>
</xsl:template>

<xsl:template match="PropertyCategory">
	<xsl:element name="row">
		<xsl:element name="entry">
			<xsl:attribute name="spanname">cj_propstbl_span_all_cols</xsl:attribute>
			<xsl:element name="emphasis">
				<xsl:value-of select="@name" />
			</xsl:element>
		</xsl:element>
	</xsl:element>
	
	<xsl:apply-templates select = "./Property"/>
</xsl:template>

<xsl:template match="Property">
	<xsl:element name="row">
		<xsl:element name="entry">
			<xsl:value-of select="@name" />
		</xsl:element>
		<xsl:element name="entry">
			<xsl:value-of select="." />
		</xsl:element>
		<xsl:element name="entry">
			<xsl:value-of select="@required" />
		</xsl:element>
		<xsl:element name="entry">
			<xsl:value-of select="@default" />
		</xsl:element>
		<xsl:element name="entry">
			<xsl:value-of select="@since" />
		</xsl:element>
	</xsl:element>
</xsl:template>

</xsl:stylesheet>