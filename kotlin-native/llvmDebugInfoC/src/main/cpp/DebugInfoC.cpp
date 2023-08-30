/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <assert.h>
#include <llvm/IR/DebugInfo.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/DIBuilder.h>
#include <llvm/IR/DebugInfoMetadata.h>
#include <llvm/IR/Instruction.h>
#include <llvm/Support/Casting.h>
#include <llvm-c/DebugInfo.h>
#include "DebugInfoC.h"
/**
 * c++ --std=c++17 llvmDebugInfoC/src/DebugInfoC.cpp -IllvmDebugInfoC/include/ -Idependencies/all/clang+llvm-3.9.0-darwin-macos/include -Ldependencies/all/clang+llvm-3.9.0-darwin-macos/lib  -lLLVMCore -lLLVMSupport -lncurses -shared -o libLLVMDebugInfoC.dylib
 */

namespace llvm {
//DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIBuilder,        DIBuilderRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DICompileUnit,    DICompileUnitRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIFile,           DIFileRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIBasicType,      DIBasicTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DICompositeType,  DICompositeTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIType,           DITypeOpaqueRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIDerivedType,    DIDerivedTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIModule,         DIModuleRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIScope,          DIScopeOpaqueRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DISubroutineType, DISubroutineTypeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DISubprogram,     DISubprogramRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DILocation,       DILocationRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DILocalVariable,  DILocalVariableRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIExpression,     DIExpressionRef)
//DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DINodeArray,      DINodeArray)

// from Module.cpp
//DEFINE_SIMPLE_CONVERSION_FUNCTIONS(Module,        LLVMModuleRef)
}

/**
 * see [DIFlags::FlagFwdDecl].
 */
#define DI_FORWARD_DECLARAION (4)
extern "C" {

void DIFinalize(DIBuilderRef builder) {
  auto diBuilder = llvm::unwrap(builder);
  diBuilder->finalize();
}

DICompileUnitRef DICreateCompilationUnit(DIBuilderRef builder, unsigned int lang,
                                         const char *file, const char* dir,
                                         const char * producer, int isOptimized,
                                         const char * flags, unsigned int rv) {
  llvm::DIBuilder *D = llvm::unwrap(builder);
  return llvm::wrap(llvm::unwrap(builder)->createCompileUnit(lang, D->createFile(file, dir), producer, isOptimized, flags, rv));
}

DIFileRef DICreateFile(DIBuilderRef builder, const char *filename, const char *directory) {
  return llvm::wrap(llvm::unwrap(builder)->createFile(filename, directory));
}

DIBasicTypeRef DICreateBasicType(DIBuilderRef builder, const char* name, uint64_t sizeInBits, uint64_t alignment, unsigned encoding) {
  return llvm::wrap(llvm::unwrap(builder)->createBasicType(name, sizeInBits, encoding));
}

DIModuleRef DICreateModule(DIBuilderRef builder, DIScopeOpaqueRef scope,
                           const char* name, const char* configurationMacro,
                           const char* includePath, const char *iSysRoot) {
  return llvm::wrap(llvm::unwrap(builder)->createModule(llvm::unwrap(scope), name, configurationMacro, includePath, iSysRoot));
}

static DISubprogramRef DICreateFunctionShared(DIBuilderRef builderRef, DIScopeOpaqueRef scope,
                                              llvm::StringRef name, llvm::StringRef linkageName,
                                              DIFileRef file, unsigned lineNo,
                                              DISubroutineTypeRef type, int isLocal,
                                              int isDefinition, unsigned scopeLine) {
  auto builder = llvm::unwrap(builderRef);
  auto subprogram = builder->createFunction(llvm::unwrap(scope),
                                            name,
                                            linkageName,
                                            llvm::unwrap(file),
                                            lineNo,
                                            llvm::unwrap(type),
                                            scopeLine, llvm::DINode::DIFlags::FlagZero, llvm::DISubprogram::toSPFlags(false, true, false));
  auto tmp = subprogram->getRetainedNodes().get();
  if (!tmp && tmp->isTemporary())
    llvm::MDTuple::deleteTemporary(tmp);

  builder->finalizeSubprogram(subprogram);
  return llvm::wrap(subprogram);
}

DISubprogramRef DICreateFunction(DIBuilderRef builderRef, DIScopeOpaqueRef scope,
                                 const char* name, const char *linkageName,
                                 DIFileRef file, unsigned lineNo,
                                 DISubroutineTypeRef type, int isLocal,
                                 int isDefinition, unsigned scopeLine) {
  return DICreateFunctionShared(builderRef, scope, name, linkageName, file, lineNo, type, isLocal, isDefinition, scopeLine);
}

DISubprogramRef DICreateBridgeFunction(DIBuilderRef builderRef, DIScopeOpaqueRef scope,
                                       LLVMValueRef function,
                                       DIFileRef file, unsigned lineNo,
                                       DISubroutineTypeRef type, int isLocal,
                                       int isDefinition, unsigned scopeLine) {
  auto fn = llvm::cast<llvm::Function>(llvm::unwrap(function));
  auto name = fn->getName();
  auto subprogram = DICreateFunctionShared(builderRef, scope, name, name, file, lineNo, type, isLocal, isDefinition, scopeLine);
  fn->setSubprogram(llvm::unwrap(subprogram));
  return subprogram;
}

DIScopeOpaqueRef DICreateLexicalBlockFile(DIBuilderRef builderRef, DIScopeOpaqueRef scopeRef, DIFileRef fileRef) {
  return llvm::wrap(llvm::unwrap(builderRef)->createLexicalBlockFile(llvm::unwrap(scopeRef), llvm::unwrap(fileRef)));
}

DIScopeOpaqueRef DICreateLexicalBlock(DIBuilderRef builderRef, DIScopeOpaqueRef scopeRef, DIFileRef fileRef, int line, int column) {
  return llvm::wrap(llvm::unwrap(builderRef)->createLexicalBlock(llvm::unwrap(scopeRef), llvm::unwrap(fileRef), line, column));
}


DICompositeTypeRef DICreateStringPointerType(
    DIBuilderRef refBuilder,
    DIDerivedTypeRef objHeaderPointerType,
    DIBasicTypeRef stringCharCountType,
    DIBasicTypeRef charUtf16Type,
    DILocalVariableRef charCount
) {
    auto builder = llvm::unwrap(refBuilder);

//    builder->createTempMacroFile()
    // Encoding???
    // Type declarations
//    auto objHeaderForString = builder->createBasicType("ObjHeader(String)", 64, llvm::dwarf::DW_ATE_signed);
//    builder->retainType(objHeaderForString);
//    auto pointerToObjHeaderForString = builder->createPointerType(objHeaderForString, 64);
//    builder->retainType(pointerToObjHeaderForString);
//    auto stringCharCountType = builder->createBasicType("StringCharCount", 64, llvm::dwarf::DW_ATE_signed);
//    builder->retainType(stringCharCountType);

//    uint64_t getCount [] = {
//            llvm::dwarf::DW_OP_push_object_address,
//            llvm::dwarf::DW_OP_plus_uconst,
//    };
//    auto getCountExpr = builder->createAutoVariable(nullptr, )
//    auto getCountVar = builder->createAutoVariable()
//    auto getCountExpr = builder->createExpression(getCount);
//    auto getCountExpr = builder->createConstantValueExpression(42);
//    auto var = builder->createAutoVariable(nullptr, "Var", nullptr, 0, stringCharCountInString, true);
//    builder->insertDeclare(nullptr, var, getCountExpr, nullptr)
//    builder->insert
//    getCountExpr.

//    llvm::dwarf::DW_Expr


//    builder->createBasicType()

//    assert(getCountExpr != nullptr);
//    builder->insertDeclare();
//    builder->createExpression();
//    llvm::Metadata::ConstantAsMetadataKind;
//    builder->createAutoVariable()


//    auto charType = builder->createBasicType("Utf16Char", 16, llvm::dwarf::DW_ATE_unsigned);
//    auto stringCharCountInString = builder->createMemberType(nullptr, "charCount", nullptr, 0, 64, 64, 64, llvm::DINode::FlagZero, llvm::unwrap(stringCharCountType));
//    builder->retainType(charType);
//    auto charCount = builder->getOrCreateSubrange(0, getCountExpr); //
//    charCount->replaceOperandWith(0, getCountExpr);
//    llvm::DISubrange::get()
//    charCount->replaceOperandWith(0, getCountExpr);
//    charCount->getRawCountNode()
//    auto charCount = builder->getOrCreateSubrange(0, 1);
//    builder->createAutoVariable()
//    builder->createCo
//    builder->createConstantValueExpression()
//builder->insertDeclare()
    auto length = builder->getOrCreateSubrange(llvm::unwrap(charCount), nullptr, nullptr, nullptr);

    auto charArrayType = builder->createArrayType(
            0, 0, llvm::unwrap(charUtf16Type),
            builder->getOrCreateArray({length})
    );

//    builder->insertDeclare(llvm::AllocaInst, )
//    llvm::unwrap(charCount)->getScope()
//    builder->insertDeclare(
//
//        llvm::unwrap(charCount),
//
//    )
//    builder->retainType(charArrayType);

    // Member declarations
//    auto pointerToObjHeaderInString = builder->createMemberType(nullptr, "pointerToObjHeader", nullptr, 0, 64, 64, 0, llvm::DINode::FlagZero, llvm::unwrap(objHeaderPointerType));
//    builder->retainType(pointerToObjHeaderInString);

//    auto charArrayTypeInString = builder->createMemberType(nullptr, "charArray", nullptr, 0, 64, 64, 128, llvm::DINode::FlagZero, charArrayType);
//    builder->retainType(charArrayTypeInString);

//    auto kotlinStringType = builder->createStructType(
//        nullptr, "KotlinString", nullptr, 0,
//        /* SizeInBits= */ 64*3,
//        /* AlignInBits= = */ 64,
//        llvm::DINode::FlagZero, nullptr,
//        builder->getOrCreateArray({
//                                          pointerToObjHeaderInString,
//                                          stringCharCountInString,
//                                          charArrayTypeInString
//        })
//    );
//    builder->retainType(kotlinStringType);

//    auto kotlinStringPointerType = builder->createPointerType(kotlinStringType, 64);
//    builder->retainType(kotlinStringPointerType);
    return llvm::wrap(charArrayType);
}


DICompositeTypeRef DICreateStructType(DIBuilderRef refBuilder,
                                      DIScopeOpaqueRef scope, const char *name,
                                      DIFileRef file, unsigned lineNumber,
                                      uint64_t sizeInBits, uint64_t alignInBits,
                                      unsigned flags, DITypeOpaqueRef derivedFrom,
                                      DIDerivedTypeRef *elements,
                                      uint64_t elementsCount,
                                      DICompositeTypeRef refPlace) {
  auto builder = llvm::unwrap(refBuilder);
  if ((flags & DI_FORWARD_DECLARAION) != 0) {
    auto tmp = builder->createReplaceableCompositeType(
        llvm::dwarf::DW_TAG_structure_type, name, llvm::unwrap(scope), llvm::unwrap(file), lineNumber, 0, sizeInBits, alignInBits,
        (llvm::DINode::DIFlags)flags);
    builder->replaceTemporary(llvm::TempDIType(tmp), tmp);
    builder->retainType(tmp);
    return llvm::wrap(tmp);
  } else {
      std::vector<llvm::Metadata *> nodesArray;
//      auto foo = llvm::ArrayRef<llvm::Metadata>(elements, 3)
      for (int i = 0; i != elementsCount; ++i) {
          nodesArray.push_back(llvm::unwrap(elements[i]));
      }
      auto tmp = builder->createStructType(
              llvm::unwrap(scope), name, llvm::unwrap(file), lineNumber, sizeInBits, alignInBits,
              llvm::DINode::FlagZero,
              nullptr, builder->getOrCreateArray(nodesArray)
     );
      builder->retainType(tmp);
      return llvm::wrap(tmp);
  }
//  else {
//      builder->createArrayType()
////      auto expr = builder->createExpression();
////      auto expr = builder->createAutoVariable();
////      builder->createReplaceableCompositeType(
////              llvm::dwarf::DW_TAG_string_type, name,
////          )
////      DICompositeType
//        builder
////        builder->createExpression();
////        builder->getOrCreateSubrange(0, );
////      builder->createArrayType()
////      expr->
////      builder->createArrayType()
//
//    auto tmp = builder->createStructType(
//        llvm::dwarf::DW_TAG_structure_type, name,
//        llvm::unwrap(file), lineNumber, sizeInBits,
//        alignInBits, (llvm::DINode::DIFlags)flags, derivedFrom, elements
//    );
//  }
  assert(false);
  return nullptr;
}


DICompositeTypeRef DICreateArrayType(DIBuilderRef refBuilder,
                                      uint64_t size, uint64_t alignInBits,
                                      DITypeOpaqueRef refType,
                                     uint64_t elementsCount) {
  auto builder = llvm::unwrap(refBuilder);
  auto range = std::vector<llvm::Metadata*>({llvm::dyn_cast<llvm::Metadata>(builder->getOrCreateSubrange(0, size))});
  auto type = builder->createArrayType(size, alignInBits, llvm::unwrap(refType),
                                                           builder->getOrCreateArray(range));
  builder->retainType(type);
  return llvm::wrap(type);
}


DIDerivedTypeRef DICreateMemberType(DIBuilderRef refBuilder,
                                    DIScopeOpaqueRef refScope,
                                    const char *name,
                                    DIFileRef file,
                                    unsigned lineNum,
                                    uint64_t sizeInBits,
                                    uint64_t alignInBits,
                                    uint64_t offsetInBits,
                                    unsigned flags,
                                    DITypeOpaqueRef type) {
  return llvm::wrap(llvm::unwrap(refBuilder)->createMemberType(
                      llvm::unwrap(refScope),
                      name,
                      llvm::unwrap(file),
                      lineNum,
                      sizeInBits,
                      alignInBits,
                      offsetInBits,
                      (llvm::DINode::DIFlags)flags,
                      llvm::unwrap(type)));
}

DICompositeTypeRef DICreateReplaceableCompositeType(DIBuilderRef refBuilder,
                                                    int tag,
                                                    const char *name,
                                                    DIScopeOpaqueRef refScope,
                                                    DIFileRef refFile,
                                                    unsigned line) {
  auto builder = llvm::unwrap(refBuilder);
  auto type = builder->createReplaceableCompositeType(
                                    tag, name, llvm::unwrap(refScope), llvm::unwrap(refFile), line);
  builder->retainType(type);
  return llvm::wrap(type);
}

DIDerivedTypeRef DICreateReferenceType(DIBuilderRef refBuilder, DITypeOpaqueRef refType) {
  auto builder = llvm::unwrap(refBuilder);
  auto type = builder->createReferenceType(
                                    llvm::dwarf::DW_TAG_reference_type,
                                    llvm::unwrap(refType));
  builder->retainType(type);
  return llvm::wrap(type);
}

DIDerivedTypeRef DICreatePointerType(DIBuilderRef refBuilder, DITypeOpaqueRef refType) {
  auto builder = llvm::unwrap(refBuilder);
  auto type = builder->createReferenceType(
                                    llvm::dwarf::DW_TAG_pointer_type,
                                    llvm::unwrap(refType));
  builder->retainType(type);
  return llvm::wrap(type);
}

DISubroutineTypeRef DICreateSubroutineType(DIBuilderRef builder,
                                           DITypeOpaqueRef* types,
                                           unsigned typesCount) {
  std::vector<llvm::Metadata *> parameterTypes;
  for (int i = 0; i != typesCount; ++i) {
    parameterTypes.push_back(llvm::unwrap(types[i]));
  }
  llvm::DIBuilder *b = llvm::unwrap(builder);
  llvm::DITypeRefArray typeArray = b->getOrCreateTypeArray(parameterTypes);
  auto type = b->createSubroutineType(typeArray);
  b->retainType(type);
  return llvm::wrap(type);
}

void DIFunctionAddSubprogram(LLVMValueRef fn, DISubprogramRef sp) {
  auto f = llvm::cast<llvm::Function>(llvm::unwrap(fn));
  auto dsp = llvm::cast<llvm::DISubprogram>(llvm::unwrap(sp));
  f->setSubprogram(dsp);
  if (!dsp->describes(f)) {
    fprintf(stderr, "error!!! f:%s, sp:%s\n", f->getName().str().c_str(), dsp->getLinkageName().str().c_str());
  }
}

DILocalVariableRef DICreateAutoVariable(DIBuilderRef builder, DIScopeOpaqueRef scope, const char *name, DIFileRef file, unsigned line, DITypeOpaqueRef type, unsigned flags) {
  return llvm::wrap(llvm::unwrap(builder)->createAutoVariable(
    llvm::unwrap(scope),
    name,
    llvm::unwrap(file),
    line,
    llvm::unwrap(type), true, (llvm::DINode::DIFlags)flags));
}

DILocalVariableRef DICreateParameterVariable(DIBuilderRef builder, DIScopeOpaqueRef scope, const char *name, unsigned argNo, DIFileRef file, unsigned line, DITypeOpaqueRef type) {
  return llvm::wrap(llvm::unwrap(builder)->createParameterVariable(
    llvm::unwrap(scope),
    name,
    argNo,
    llvm::unwrap(file),
    line,
    llvm::unwrap(type)));
}

DIExpressionRef DICreateEmptyExpression(DIBuilderRef builder) {
  return llvm::wrap(llvm::unwrap(builder)->createExpression());
}

void DIInsertDeclaration(DIBuilderRef builder, LLVMValueRef value, DILocalVariableRef localVariable, DILocationRef location, LLVMBasicBlockRef bb, int64_t *expr, uint64_t exprCount) {
  auto di_builder = llvm::unwrap(builder);
  std::vector<int64_t> expression;
  for (uint64_t i = 0; i < exprCount; ++i)
    expression.push_back(expr[i]);
  di_builder->insertDeclare(llvm::unwrap(value),
                            llvm::unwrap(localVariable),
                            di_builder->createExpression(expression),
                            llvm::unwrap(location),
                            llvm::unwrap(bb));
}

DILocationRef LLVMCreateLocation(LLVMContextRef contextRef, unsigned line,
                                 unsigned col, DIScopeOpaqueRef scope) {
  auto location = llvm::DILocation::get(*llvm::unwrap(contextRef), line, col, llvm::unwrap(scope), nullptr);
  return llvm::wrap(location);
}

DILocationRef LLVMCreateLocationInlinedAt(LLVMContextRef contextRef, unsigned line,
                                 unsigned col, DIScopeOpaqueRef scope, DILocationRef refLocationInlinedAt) {
  auto location = llvm::DILocation::get(*llvm::unwrap(contextRef), line, col, llvm::unwrap(scope), llvm::unwrap(refLocationInlinedAt));
  return llvm::wrap(location);
}

void LLVMBuilderSetDebugLocation(LLVMBuilderRef builder, DILocationRef refLocation) {
  llvm::unwrap(builder)->SetCurrentDebugLocation(llvm::unwrap(refLocation));
}

void LLVMBuilderResetDebugLocation(LLVMBuilderRef builder) {
  llvm::unwrap(builder)->SetCurrentDebugLocation(nullptr);
}

LLVMValueRef LLVMBuilderGetCurrentFunction(LLVMBuilderRef builder) {
  return llvm::wrap(llvm::unwrap(builder)->GetInsertBlock()->getParent());
}

int DISubprogramDescribesFunction(DISubprogramRef sp, LLVMValueRef fn) {
  return llvm::unwrap(sp)->describes(llvm::cast<llvm::Function>(llvm::unwrap(fn)));
}
} /* extern "C" */

