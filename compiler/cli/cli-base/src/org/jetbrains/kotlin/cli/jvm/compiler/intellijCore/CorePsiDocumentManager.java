/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.intellijCore;

import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import org.jetbrains.annotations.NotNull;

final class CorePsiDocumentManager extends PsiDocumentManagerBase {
    CorePsiDocumentManager(@NotNull Project project) {
        super(project);
    }
}
