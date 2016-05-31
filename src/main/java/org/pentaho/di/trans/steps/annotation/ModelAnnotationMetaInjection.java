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
import org.pentaho.agilebi.modeler.models.annotations.AnnotationType;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelProperty;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.StepInjectionMetaEntry;
import org.pentaho.di.trans.step.StepMetaInjectionInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Rowell Belen
 */
public class ModelAnnotationMetaInjection implements StepMetaInjectionInterface {
  private final ModelAnnotationMeta meta;
  private List<Class<? extends AnnotationType>> supportedAnnotationTypes;

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

    if ( CollectionUtils.isNotEmpty( supportedAnnotationTypes ) ) {
      for ( Class<? extends AnnotationType> annotationType : supportedAnnotationTypes ) {
        try {
          AnnotationType instance = annotationType.newInstance();
          entries.add( createMdiGroup( instance ) );
        } catch ( InstantiationException | IllegalAccessException e ) {
          // TODO: log it
          e.printStackTrace();
        }
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
        default:
          supportedAnnotationTypes.stream().filter( annotationType -> key.equals( annotationType.getSimpleName() ) )
            .forEach( annotationType -> {
              try {
                injectMdiGroup( stepInjectionMetaEntry, annotationType, modelAnnotationGroup );
              } catch ( IllegalAccessException | InstantiationException e ) {
                e.printStackTrace();
              }
            } );
      }
    } );

    meta.setModelAnnotations( modelAnnotationGroup );

  }

  @Override public List<StepInjectionMetaEntry> extractStepMetadataEntries() throws KettleException {
    // TODO
    return null;
  }

  public void setSupportedAnnotationTypes( List<Class<? extends AnnotationType>> supportedAnnotationTypes ) {
    this.supportedAnnotationTypes = supportedAnnotationTypes;
  }

  public List<Class<? extends AnnotationType>> getSupportedAnnotationTypes() {
    return supportedAnnotationTypes;
  }

  protected StepInjectionMetaEntry createMdiGroup( AnnotationType type ) {
    StepInjectionMetaEntry attributesGroup =
      new StepInjectionMetaEntry( type.getClass().getSimpleName(), ValueMetaInterface.TYPE_NONE, type.getClass().getSimpleName() );

    // list of data for the specific annotation
    List<StepInjectionMetaEntry> values = new ArrayList<>();

    List<ModelProperty> modelProperties = type.getModelProperties();
    values.addAll( modelProperties.stream()
      .map( prop -> new StepInjectionMetaEntry( type.getClass().getSimpleName() + "_" + prop.name(), ValueMetaInterface.TYPE_STRING, prop.name() ) )
      .collect( Collectors.toList() ) );

    StepInjectionMetaEntry mac =
      new StepInjectionMetaEntry( type.getClass().getSimpleName() + "_container", ValueMetaInterface.TYPE_NONE, "" );

    // add the real entries as details to this intermediate entry
    mac.getDetails().addAll( values );

    attributesGroup.getDetails().add( mac );
    return attributesGroup;
  }

  /**
   * Injects a stream annotation into the ModelAnnotationGroup provided based on the metadata given
   * @param groupEntry               The parent grouping container that contains metadata for a particular type of annotation
   * @param type                     The specific class of AnnotationType to inject
   * @param modelAnnotationGroup     The ModelAnnotationGroup that should contain the instance of annotation requested once injected
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  protected void injectMdiGroup( StepInjectionMetaEntry groupEntry, Class<? extends AnnotationType> type, ModelAnnotationGroup modelAnnotationGroup )
    throws IllegalAccessException, InstantiationException {

    List<StepInjectionMetaEntry> attributesGroup = groupEntry.getDetails();
    for ( StepInjectionMetaEntry injectedAnnotation : attributesGroup ) {
      // we should be getting one `injectedAnnotation` per annotation of the specified type to inject
      List<StepInjectionMetaEntry> details = injectedAnnotation.getDetails();

      // TODO: try to find a logically equivalent annotation already in the group, use that if it exists

      // create a new annotation to inject
      ModelAnnotation modelAnnotation = findExistingAnnotation( injectedAnnotation, type, modelAnnotationGroup );
      AnnotationType annotationType;
      boolean isNew = false;
      if ( modelAnnotation == null ) {
        // it's a new one
        modelAnnotation = new ModelAnnotation();
        annotationType = type.newInstance();
        modelAnnotation.setAnnotation( annotationType );
        isNew = true;
      } else {
        annotationType = modelAnnotation.getAnnotation();
      }

      for ( StepInjectionMetaEntry detail : details ) {
        try {
          String keyNoPrefix = detail.getKey().replace( type.getSimpleName() + "_", "" );
          annotationType.setModelPropertyByName( keyNoPrefix, detail.getValue() );
        } catch ( Exception e ) {
          // TODO: log it
          e.printStackTrace();
        }
      }
      if ( isNew ) {
        modelAnnotationGroup.add( modelAnnotation );
      }
    }
  }

  protected ModelAnnotation findExistingAnnotation( StepInjectionMetaEntry annotationEntry, Class<? extends AnnotationType> type, ModelAnnotationGroup modelAnnotationGroup ) {

    StepInjectionMetaEntry first = annotationEntry.getDetails().stream().filter( e -> {
      String keyNoPrefix = e.getKey().replace( type.getSimpleName() + "_", "" );
      return keyNoPrefix.equalsIgnoreCase( "attribute name" );
    } ).findFirst().orElse( null );

    String entryName = first == null ? null : first.getValue().toString();

    List<ModelAnnotation> matches = modelAnnotationGroup.stream()
      .filter( modelAnnotation ->
        modelAnnotation.getAnnotation().getClass().equals( type )
          && modelAnnotation.getAnnotation().getName().equalsIgnoreCase( entryName ) )
      .collect( Collectors.toList() );

    return CollectionUtils.isNotEmpty( matches ) ? matches.get( 0 ) : null;
  }

}
