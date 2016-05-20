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

import org.apache.commons.collections.CollectionUtils;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.StepInjectionMetaEntry;
import org.pentaho.di.trans.step.StepMetaInjectionInterface;

import java.util.ArrayList;
import java.util.List;

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


    // list of annotations
    List<StepInjectionMetaEntry> annotationEntries = new ArrayList<>();
    for ( int i = 0; i < meta.getModelAnnotations().size(); i++ ) {

      ModelAnnotation modelAnnotation = meta.getModelAnnotations().get( i );

      // add a 'grouping' entry
      StepInjectionMetaEntry container = new StepInjectionMetaEntry( modelAnnotation.getType().name(), null, ValueMetaInterface.TYPE_NONE, "annotation type" );
      entries.add( container );

      // list of data for the specific annotation
      List<StepInjectionMetaEntry> annotationValues = new ArrayList<>();

      switch ( modelAnnotation.getType() ) {
        case CREATE_ATTRIBUTE:
          CreateAttribute attribute = (CreateAttribute) modelAnnotation.getAnnotation();
          annotationValues.add( new StepInjectionMetaEntry( "FIELD_NAME", attribute.getField(), ValueMetaInterface.TYPE_STRING, "Field" ) );
          annotationValues.add( new StepInjectionMetaEntry( "DIMENSION", attribute.getDimension(), ValueMetaInterface.TYPE_STRING, "Dimension" ) );
          annotationValues.add( new StepInjectionMetaEntry( "HIERARCHY", attribute.getHierarchy(), ValueMetaInterface.TYPE_STRING, "Hierarchy" ) );
          annotationValues.add( new StepInjectionMetaEntry( "LEVEL", attribute.getLevel(), ValueMetaInterface.TYPE_STRING, "Level" ) );
          break;
        case CREATE_MEASURE:
          CreateMeasure measure = (CreateMeasure) modelAnnotation.getAnnotation();
          annotationValues.add( new StepInjectionMetaEntry( "FIELD_NAME", measure.getField(), ValueMetaInterface.TYPE_STRING, "Field" ) );
          annotationValues.add( new StepInjectionMetaEntry( "AGGREGATION_TYPE", measure.getAggregateType().name(), ValueMetaInterface.TYPE_STRING, "Type of aggregation" ) );
          break;
        default:
          break;
      }

      if ( CollectionUtils.isNotEmpty( annotationValues ) ) {
        // create an intermediate entry to add the actual injection entries to. the legacy MDI support is quirky in how it
        // expects this data to be structured.
        StepInjectionMetaEntry ma =
          new StepInjectionMetaEntry( modelAnnotation.getType().name(), null, ValueMetaInterface.TYPE_NONE, "annotation type" );

        // add the real entries as details to this intermediate entry
        ma.getDetails().addAll( annotationValues );

        container.getDetails().add( ma );
      }

    }

    return entries;
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
        case "CREATE_ATTRIBUTE":
          List<StepInjectionMetaEntry> details = stepInjectionMetaEntry.getDetails().get( 0 ).getDetails();
          for ( StepInjectionMetaEntry detail : details ) {
            if ( "FIELD_NAME".equals( detail.getKey() ) ) {
              CreateAttribute ca = new CreateAttribute();
              ca.setName( detail.getValue().toString() );
              ModelAnnotation<CreateAttribute> modelAnnotation = new ModelAnnotation<CreateAttribute>();
              modelAnnotation.setName( ca.getName() );
              modelAnnotation.setAnnotation( ca );
            }
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
