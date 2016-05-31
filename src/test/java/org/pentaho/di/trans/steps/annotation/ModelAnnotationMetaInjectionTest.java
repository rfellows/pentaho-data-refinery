/*
 * ******************************************************************************
 *
 * Copyright (C) 2002-2016 by Pentaho : http://www.pentaho.com
 *
 * ******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pentaho.di.trans.steps.annotation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.agilebi.modeler.models.annotations.AnnotationType;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateCalculatedMember;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.LinkDimension;
import org.pentaho.di.trans.step.StepInjectionMetaEntry;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by rfellows on 5/26/16.
 */
@RunWith( MockitoJUnitRunner.class )
public class ModelAnnotationMetaInjectionTest {

  ModelAnnotationMetaInjection mdi;
  ModelAnnotationMeta meta = new ModelAnnotationMeta();

  List<Class<? extends AnnotationType>> types;

  @Before
  public void setUp() throws Exception {
    meta.setDefault();
    mdi = new ModelAnnotationMetaInjection( meta );

    types = Arrays.asList( new Class[] {
      CreateAttribute.class,
      CreateMeasure.class,
      CreateCalculatedMember.class,
      LinkDimension.class } );
  }

  @Test
  public void testGetStepInjectionMetadataEntries() throws Exception {
    List<StepInjectionMetaEntry> metadataEntries = mdi.getStepInjectionMetadataEntries();
    assertNotNull( metadataEntries );
    int entryCount = metadataEntries.size();
    assertTrue( entryCount > 0 );

  }

  @Test
  public void testSettingSupportedAnnotationTypes() throws Exception {
    mdi.setSupportedAnnotationTypes( types );
    assertEquals( types, mdi.getSupportedAnnotationTypes() );
  }

  @Test
  public void testAnnotationTypesTurnIntoInjectableThings() throws Exception {
    List<StepInjectionMetaEntry> metadataEntries = mdi.getStepInjectionMetadataEntries();
    int entryCount = metadataEntries.size();

    mdi.setSupportedAnnotationTypes( Arrays.asList( new Class[] { CreateMeasure.class } ) );
    metadataEntries = mdi.getStepInjectionMetadataEntries();
    assertEquals( entryCount + 1, metadataEntries.size() );
    CreateMeasure cm = new CreateMeasure();

    // make sure all of the properties for create measure have been added in the correct place
    // assume last element of the root List is our create measure, it's details is a hidden container layer, it's details
    // is where our properties should live
    assertEquals( metadataEntries.get( entryCount ).getDetails().get( 0 ).getDetails().size(), cm.getModelProperties().size() );
  }
}
