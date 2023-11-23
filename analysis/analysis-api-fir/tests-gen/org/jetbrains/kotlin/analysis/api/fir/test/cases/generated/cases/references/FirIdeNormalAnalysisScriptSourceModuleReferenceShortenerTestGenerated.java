/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.generated.cases.references;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisSessionMode;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode;
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.AbstractReferenceShortenerTest;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("analysis/analysis-api/testData/components/referenceShortener/referenceShortener")
@TestDataPath("$PROJECT_ROOT")
public class FirIdeNormalAnalysisScriptSourceModuleReferenceShortenerTestGenerated extends AbstractReferenceShortenerTest {
    @NotNull
    @Override
    public AnalysisApiTestConfigurator getConfigurator() {
        return AnalysisApiFirTestConfiguratorFactory.INSTANCE.createConfigurator(
            new AnalysisApiTestConfiguratorFactoryData(
                FrontendKind.Fir,
                TestModuleKind.ScriptSource,
                AnalysisSessionMode.Normal,
                AnalysisApiMode.Ide
            )
        );
    }

    @Test
    public void testAllFilesPresentInReferenceShortener() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/referenceShortener/referenceShortener"), Pattern.compile("^(.+)\\.kts$"), null, true);
    }

    @Test
    @TestMetadata("callInsideScriptExpression.kts")
    public void testCallInsideScriptExpression() throws Exception {
        runTest("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/callInsideScriptExpression.kts");
    }

    @Nested
    @TestMetadata("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/nestedClasses")
    @TestDataPath("$PROJECT_ROOT")
    public class NestedClasses {
        @Test
        public void testAllFilesPresentInNestedClasses() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/nestedClasses"), Pattern.compile("^(.+)\\.kts$"), null, true);
        }

        @Nested
        @TestMetadata("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/nestedClasses/classHeaderPositions")
        @TestDataPath("$PROJECT_ROOT")
        public class ClassHeaderPositions {
            @Test
            public void testAllFilesPresentInClassHeaderPositions() throws Exception {
                KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/nestedClasses/classHeaderPositions"), Pattern.compile("^(.+)\\.kts$"), null, true);
            }
        }
    }

    @Nested
    @TestMetadata("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/thisReference")
    @TestDataPath("$PROJECT_ROOT")
    public class ThisReference {
        @Test
        public void testAllFilesPresentInThisReference() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/thisReference"), Pattern.compile("^(.+)\\.kts$"), null, true);
        }

        @Nested
        @TestMetadata("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/thisReference/withLabel")
        @TestDataPath("$PROJECT_ROOT")
        public class WithLabel {
            @Test
            public void testAllFilesPresentInWithLabel() throws Exception {
                KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/thisReference/withLabel"), Pattern.compile("^(.+)\\.kts$"), null, true);
            }
        }
    }

    @Nested
    @TestMetadata("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/typeParameters")
    @TestDataPath("$PROJECT_ROOT")
    public class TypeParameters {
        @Test
        public void testAllFilesPresentInTypeParameters() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/referenceShortener/referenceShortener/typeParameters"), Pattern.compile("^(.+)\\.kts$"), null, true);
        }
    }
}
