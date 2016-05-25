/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
 *
 * *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ********************************************************************************/

package org.pentaho.di.trans.steps.annotation;

import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelProperty;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.StepInjectionMetaEntry;
import org.pentaho.di.trans.step.StepMetaInjectionInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Rowell Belen
 */
public class ModelAnnotationMetaInjection implements StepMetaInjectionInterface {
  private final ModelAnnotationMeta meta;

  public ModelAnnotationMetaInjection( ModelAnnotationMeta meta ) {
    this.meta = meta;
  }

  @Override public List<StepInjectionMetaEntry> getStepInjectionMetadataEntries() throws KettleException {
    List<StepInjectionMetaEntry> entries = new ArrayList<>();

    entries.add( new StepInjectionMetaEntry(
      "MODEL_ANNOTATION_CATEGORY",
      meta.getModelAnnotationCategory(),
      ValueMetaInterface.TYPE_STRING,
      "category" ) );

    entries.add( new StepInjectionMetaEntry(
      "MODEL_ANNOTATION_GROUP_NAME",
      meta.getModelAnnotations().getName(),
      ValueMetaInterface.TYPE_STRING,
      "group name" ) );


    entries.add( createMeasuresMdiGroup() );
    entries.add( createAttributesMdiGroup() );

    return entries;
  }

  protected StepInjectionMetaEntry createMeasuresMdiGroup() {
    StepInjectionMetaEntry measuresGroup =
      new StepInjectionMetaEntry( "MEASURES", ValueMetaInterface.TYPE_NONE, "Create Measure Annotations" );
    // list of data for the specific annotation
    List<StepInjectionMetaEntry> annotationValues = new ArrayList<>();

    List<ModelProperty> modelProperties = new CreateMeasure().getModelProperties();
    annotationValues.addAll( modelProperties.stream()
      .map( prop -> new StepInjectionMetaEntry( "M_" + prop.name(), ValueMetaInterface.TYPE_STRING, "" ) )
      .collect( Collectors.toList() ) );

    StepInjectionMetaEntry ma =
      new StepInjectionMetaEntry( "measuresContainer", ValueMetaInterface.TYPE_NONE, "" );

    // add the real entries as details to this intermediate entry
    ma.getDetails().addAll( annotationValues );

    measuresGroup.getDetails().add( ma );
    return measuresGroup;
  }

  protected StepInjectionMetaEntry createAttributesMdiGroup() {
    StepInjectionMetaEntry attributesGroup =
      new StepInjectionMetaEntry( "ATTRIBUTES", ValueMetaInterface.TYPE_NONE, "Create Measure Annotations" );
    // list of data for the specific annotation
    List<StepInjectionMetaEntry> attributeValues = new ArrayList<>();

    List<ModelProperty> modelProperties = new CreateAttribute().getModelProperties();
    attributeValues.addAll( modelProperties.stream()
      .map( prop -> new StepInjectionMetaEntry( "A_" + prop.name(), ValueMetaInterface.TYPE_STRING, prop.name() ) )
      .collect( Collectors.toList() ) );

    StepInjectionMetaEntry mac =
      new StepInjectionMetaEntry( "attributesContainer", ValueMetaInterface.TYPE_NONE, "" );

    // add the real entries as details to this intermediate entry
    mac.getDetails().addAll( attributeValues );

    attributesGroup.getDetails().add( mac );
    return attributesGroup;
  }

  @Override public void injectStepMetadataEntries( List<StepInjectionMetaEntry> metadata ) throws KettleException {
    // if the model annotation group is already available, use it. otherwise create a new one
    ModelAnnotationGroup modelAnnotationGroup = meta.getModelAnnotations() == null ? new ModelAnnotationGroup() : meta.getModelAnnotations();

    metadata.forEach( stepInjectionMetaEntry -> {
      String key = stepInjectionMetaEntry.getKey();
      String value = stepInjectionMetaEntry.getValue() == null ? null : (String) stepInjectionMetaEntry.getValue();
      switch ( key ) {
        case "MODEL_ANNOTATION_CATEGORY":
          meta.setModelAnnotationCategory( value );
          break;
        case "MODEL_ANNOTATION_GROUP_NAME":
          modelAnnotationGroup.setName( value );
          break;
        case "MEASURES":
          List<StepInjectionMetaEntry> measuresGroup = stepInjectionMetaEntry.getDetails();
          for ( StepInjectionMetaEntry injectedAnnotation : measuresGroup ) {
            List<StepInjectionMetaEntry> details = injectedAnnotation.getDetails();

            CreateMeasure cm = new CreateMeasure();
            ModelAnnotation measureAnnotation = new ModelAnnotation<CreateMeasure>();
            measureAnnotation.setAnnotation( cm );

            for ( StepInjectionMetaEntry detail : details ) {
              try {
                cm.setModelPropertyByName( detail.getKey().replace( "M_", "" ), detail.getValue() );
              } catch ( Exception e ) {
                e.printStackTrace();
              }
            }
            modelAnnotationGroup.add( measureAnnotation );
          }
          break;

        case "ATTRIBUTES":
          List<StepInjectionMetaEntry> attributesGroup = stepInjectionMetaEntry.getDetails();
          for ( StepInjectionMetaEntry injectedAnnotation : attributesGroup ) {
            List<StepInjectionMetaEntry> details = injectedAnnotation.getDetails();

            ModelAnnotation attributeAnnotation = new ModelAnnotation<CreateAttribute>();
            CreateAttribute ca = new CreateAttribute();
            attributeAnnotation.setAnnotation( ca );

            for ( StepInjectionMetaEntry detail : details ) {
              try {
                ca.setModelPropertyByName( detail.getKey().replace( "A_", "" ), detail.getValue() );
              } catch ( Exception e ) {
                e.printStackTrace();
              }
            }
            modelAnnotationGroup.add( attributeAnnotation );
          }
          break;
        default:
          System.out.println( key + " = " + value );
      }
    } );

    meta.setModelAnnotations( modelAnnotationGroup );

  }

  @Override public List<StepInjectionMetaEntry> extractStepMetadataEntries() throws KettleException {
    // TODO
    return null;
  }
}
