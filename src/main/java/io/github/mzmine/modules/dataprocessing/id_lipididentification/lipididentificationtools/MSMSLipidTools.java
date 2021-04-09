/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;

/**
 * This class contains methods for MS/MS lipid identifications
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
/**
 * @author aiden
 *
 */
public class MSMSLipidTools {

  private static final ChainTools CHAIN_TOOLS = new ChainTools();
  private static final LipidFactory LIPID_FACTORY = new LipidFactory();


  public LipidFragment checkForClassSpecificFragment(Range<Double> mzTolRangeMSMS,
      ILipidAnnotation lipidAnnotation, IonizationType ionizationType,
      LipidFragmentationRule[] rules, DataPoint dataPoint, Scan msMsScan) {
    for (int i = 0; i < rules.length; i++) {
      if (!ionizationType.equals(rules[i].getIonizationType())) {
        continue;
      }
      LipidFragment detectedFragment =
          checkForSpecificRuleTpye(rules[i], mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      if (detectedFragment != null) {
        return detectedFragment;
      }
    }
    return null;
  }

  private LipidFragment checkForSpecificRuleTpye(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    switch (ruleType) {
      case HEADGROUP_FRAGMENT:
        return checkForHeadgroupFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case HEADGROUP_FRAGMENT_NL:
        return checkForHeadgroupFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case ACYLCHAIN_FRAGMENT:
        return checkForAcylChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case ACYLCHAIN_FRAGMENT_NL:
        return checkForAcylChainFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT:
        return checkForAcylChainMinusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ACYLCHAIN_MINUS_FORMULA_FRAGMENT_NL:
        return checkForAcylChainMinusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT:
        return checkForAcylChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ACYLCHAIN_PLUS_FORMULA_FRAGMENT_NL:
        return checkForAcylChainPlusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case TWO_ACYLCHAINS_PLUS_FORMULA_FRAGMENT:
        return checkForTwoAcylChainsPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ALKYLCHAIN_FRAGMENT:
        return checkForAlkylChainFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case ALKYLCHAIN_FRAGMENT_NL:
        return checkForAlkylChainFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint,
            msMsScan);
      case ALKYLCHAIN_MINUS_FORMULA_FRAGMENT:
        return checkForAlkylChainMinusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ALKYLCHAIN_MINUS_FORMULA_FRAGMENT_NL:
        return checkForAlkylChainMinusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ALKYLCHAIN_PLUS_FORMULA_FRAGMENT:
        return checkForAlkylChainPlusFormulaFragment(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      case ALKYLCHAIN_PLUS_FORMULA_FRAGMENT_NL:
        return checkForAlkylChainPlusFormulaFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation,
            dataPoint, msMsScan);
      default:
        return null;
    }
  }

  private LipidFragment checkForHeadgroupFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    if (mzTolRangeMSMS.contains(mzFragmentExact)) {
      return new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), mzFragmentExact, dataPoint,
          lipidAnnotation.getLipidClass(), null, null, null, msMsScan);
    } else {
      return null;
    }
  }

  private LipidFragment checkForHeadgroupFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzExact = mzPrecursorExact - mzFragmentExact;
    if (mzTolRangeMSMS.contains(mzExact)) {
      return new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
          lipidAnnotation.getLipidClass(), null, null, LipidChainType.ACYL_CHAIN, msMsScan);
    } else {
      return null;
    }
  }

  // Acly Chains
  private LipidFragment checkForAcylChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
    for (String fattyAcidFormula : fattyAcidFormulas) {
      Double mzExact = FormulaUtils.calculateExactMass(fattyAcidFormula)
          + IonizationType.NEGATIVE_HYDROGEN.getAddedMass();
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAcylChainFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    for (String fattyAcidFormula : fattyAcidFormulas) {
      Double mzFattyAcid = FormulaUtils.calculateExactMass(fattyAcidFormula);
      Double mzExact = mzPrecursorExact - mzFattyAcid;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }


  private LipidFragment checkForAcylChainMinusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
    for (String fattyAcidFormula : fattyAcidFormulas) {
      FormulaUtils.ionizeFormula(fattyAcidFormula, IonizationType.NEGATIVE_HYDROGEN, 1);
      Double mzExact = FormulaUtils.calculateExactMass(fattyAcidFormula) - mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAcylChainMinusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
    for (String fattyAcidFormula : fattyAcidFormulas) {
      FormulaUtils.ionizeFormula(fattyAcidFormula, IonizationType.NEGATIVE_HYDROGEN, 1);
      Double mzExact =
          mzPrecursorExact - FormulaUtils.calculateExactMass(fattyAcidFormula) - mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAcylChainPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
    for (String fattyAcidFormula : fattyAcidFormulas) {
      FormulaUtils.ionizeFormula(fattyAcidFormula, IonizationType.NEGATIVE_HYDROGEN, 1);
      Double mzExact = FormulaUtils.calculateExactMass(fattyAcidFormula) + mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAcylChainPlusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> fattyAcidFormulas = CHAIN_TOOLS.calculateFattyAcidFormulas();
    for (String fattyAcidFormula : fattyAcidFormulas) {
      FormulaUtils.ionizeFormula(fattyAcidFormula, IonizationType.NEGATIVE_HYDROGEN, 1);
      Double mzExact =
          mzPrecursorExact - FormulaUtils.calculateExactMass(fattyAcidFormula) + mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForTwoAcylChainsPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> fattyAcidFormulasOne = CHAIN_TOOLS.calculateFattyAcidFormulas();
    List<String> fattyAcidFormulasTwo = CHAIN_TOOLS.calculateFattyAcidFormulas();
    for (int i = 0; i < fattyAcidFormulasOne.size(); i++) {
      FormulaUtils.ionizeFormula(fattyAcidFormulasOne.get(i), IonizationType.NEGATIVE_HYDROGEN, 1);
      Double mzFattyAcidOne = FormulaUtils.calculateExactMass(fattyAcidFormulasOne.get(i));
      for (int j = 0; j < fattyAcidFormulasTwo.size(); j++) {
        FormulaUtils.ionizeFormula(fattyAcidFormulasOne.get(j), IonizationType.NEGATIVE_HYDROGEN,
            1);
        Double mzFattyAcidTwo = FormulaUtils.calculateExactMass(fattyAcidFormulasTwo.get(j));
        Double mzExact = mzFattyAcidOne + mzFattyAcidTwo + mzFragmentExact;
        if (mzTolRangeMSMS.contains(mzFattyAcidOne + mzFattyAcidTwo + mzFragmentExact)) {
          return new LipidFragment(rule.getLipidFragmentationRuleType(),
              rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
              lipidAnnotation.getLipidClass(), null, null, null, msMsScan);
        }
      }
    }
    return null;
  }

  // Alkyl Chains
  private LipidFragment checkForAlkylChainFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    for (String chainFormula : chainFormulas) {
      FormulaUtils.ionizeFormula(chainFormula, IonizationType.NEGATIVE_HYDROGEN, 1);
      Double mzExact = FormulaUtils.calculateExactMass(chainFormula);
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ALKYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    for (String chainFormula : chainFormulas) {
      Double mzFattyAcid = FormulaUtils.calculateExactMass(chainFormula);
      Double mzExact = mzPrecursorExact - mzFattyAcid;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ALKYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainMinusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    for (String chainFormula : chainFormulas) {
      FormulaUtils.ionizeFormula(chainFormula, IonizationType.NEGATIVE_HYDROGEN, 1);
      Double mzExact = FormulaUtils.calculateExactMass(chainFormula) - mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ALKYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainMinusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    for (String chainFormula : chainFormulas) {
      FormulaUtils.ionizeFormula(chainFormula, IonizationType.NEGATIVE_HYDROGEN, 1);
      Double mzExact =
          mzPrecursorExact - FormulaUtils.calculateExactMass(chainFormula) - mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainPlusFormulaFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    for (String chainFormula : chainFormulas) {
      FormulaUtils.ionizeFormula(chainFormula, IonizationType.NEGATIVE_HYDROGEN, 1);
      Double mzExact = FormulaUtils.calculateExactMass(chainFormula) + mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  private LipidFragment checkForAlkylChainPlusFormulaFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {

    String fragmentFormula = rule.getMolecularFormula();
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    List<String> chainFormulas = CHAIN_TOOLS.calculateHydroCarbonFormulas();
    for (String chainFormula : chainFormulas) {
      FormulaUtils.ionizeFormula(chainFormula, IonizationType.NEGATIVE_HYDROGEN, 1);
      Double mzExact =
          mzPrecursorExact - FormulaUtils.calculateExactMass(chainFormula) + mzFragmentExact;
      if (mzTolRangeMSMS.contains(mzExact)) {
        int chainLength = CHAIN_TOOLS.getChainLengthFromFormula(chainFormula);
        int numberOfDoubleBonds = CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(chainFormula);
        return new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            LipidChainType.ACYL_CHAIN, msMsScan);
      }
    }
    return null;
  }

  public MatchedLipid confirmSpeciesLevelAnnotation(Double accurateMz,
      ILipidAnnotation lipidAnnotation, Set<LipidFragment> listOfAnnotatedFragments,
      DataPoint[] massList, double minMsMsScore, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {
    Set<LipidFragment> speciesLevelFragments = new HashSet<>();
    for (LipidFragment lipidFragment : listOfAnnotatedFragments) {
      if (lipidFragment.getLipidFragmentInformationLevelType()
          .equals(LipidAnnotationLevel.SPECIES_LEVEL)) {
        speciesLevelFragments.add(lipidFragment);
      }
    }
    if (!speciesLevelFragments.isEmpty()) {
      Double msMsScore =
          calculateMsMsScore(massList, speciesLevelFragments, minMsMsScore, mzTolRangeMSMS);
      if (msMsScore >= minMsMsScore) {
        return new MatchedLipid(lipidAnnotation, accurateMz, ionizationType,
            listOfAnnotatedFragments, msMsScore);
      }
    }
    return null;
  }

  /**
   * This methods tries to reconstruct a possible chain composition of the annotated lipid using the
   * annotated MS/MS fragments
   */
  public Set<MatchedLipid> predictMolecularSpeciesLevelAnnotation(
      Set<LipidFragment> listOfDetectedFragments, ILipidAnnotation lipidAnnotation,
      Double accurateMz, DataPoint[] massList, double minMsMsScore, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {

    List<ILipidChain> chains = getChainsFromFragments(listOfDetectedFragments);
    Set<MatchedLipid> matchedMolecularSpeciesLevelAnnotations = new HashSet<>();
    // get number of total C atoms, double bonds and number of chains

    int totalNumberOfCAtoms = 0;
    int totalNumberOfDBEs = 0;
    if (lipidAnnotation instanceof SpeciesLevelAnnotation) {
      totalNumberOfCAtoms = ((SpeciesLevelAnnotation) lipidAnnotation).getNumberOfCarbons();
      totalNumberOfDBEs = ((SpeciesLevelAnnotation) lipidAnnotation).getNumberOfDBEs();
    }
    int chainsInLipid = lipidAnnotation.getLipidClass().getChainTypes().length;

    for (int i = 0; i < chains.size(); i++) {
      int carbonOne = chains.get(i).getNumberOfCarbons();
      int dbeOne = chains.get(i).getNumberOfDBEs();
      if (chainsInLipid == 1 && carbonOne == totalNumberOfCAtoms && dbeOne == totalNumberOfDBEs) {
        List<ILipidChain> predictedChains = new ArrayList<>();
        predictedChains.add(chains.get(i));
        if (checkChainTypesFitLipidClass(predictedChains, lipidAnnotation.getLipidClass())) {
          matchedMolecularSpeciesLevelAnnotations.add(buildNewMolecularSpeciesLevelMatch(
              listOfDetectedFragments, lipidAnnotation, accurateMz, massList, predictedChains,
              minMsMsScore, mzTolRangeMSMS, ionizationType));
        }
      }
      if (chainsInLipid >= 2) {
        for (int j = 0; j < chains.size(); j++) {
          int carbonTwo = chains.get(j).getNumberOfCarbons();
          int dbeTwo = chains.get(j).getNumberOfDBEs();
          if (chainsInLipid == 2 && carbonOne + carbonTwo == totalNumberOfCAtoms
              && dbeOne + dbeTwo == totalNumberOfDBEs) {
            List<ILipidChain> predictedChains = new ArrayList<>();
            predictedChains.add(chains.get(i));
            predictedChains.add(chains.get(j));
            if (checkChainTypesFitLipidClass(predictedChains, lipidAnnotation.getLipidClass())) {
              matchedMolecularSpeciesLevelAnnotations.add(buildNewMolecularSpeciesLevelMatch(
                  listOfDetectedFragments, lipidAnnotation, accurateMz, massList, predictedChains,
                  minMsMsScore, mzTolRangeMSMS, ionizationType));
            }
          }
          if (chainsInLipid >= 3) {
            for (int k = 0; k < chains.size(); k++) {
              int carbonThree = chains.get(k).getNumberOfCarbons();
              int dbeThree = chains.get(k).getNumberOfDBEs();
              if (chainsInLipid == 3 && carbonOne + carbonTwo + carbonThree == totalNumberOfCAtoms
                  && dbeOne + dbeTwo + dbeThree == totalNumberOfDBEs) {
                List<ILipidChain> predictedChains = new ArrayList<>();
                predictedChains.add(chains.get(i));
                predictedChains.add(chains.get(j));
                predictedChains.add(chains.get(k));
                if (checkChainTypesFitLipidClass(predictedChains,
                    lipidAnnotation.getLipidClass())) {
                  matchedMolecularSpeciesLevelAnnotations.add(buildNewMolecularSpeciesLevelMatch(
                      listOfDetectedFragments, lipidAnnotation, accurateMz, massList,
                      predictedChains, minMsMsScore, mzTolRangeMSMS, ionizationType));
                }
              }
            }
          }
        }
      }
    }
    return matchedMolecularSpeciesLevelAnnotations;
  }

  private MatchedLipid buildNewMolecularSpeciesLevelMatch(
      Set<LipidFragment> listOfDetectedFragments, ILipidAnnotation lipidAnnotation,
      Double accurateMz, DataPoint[] massList, List<ILipidChain> predictedChains,
      double minMsMsScore, MZTolerance mzTolRangeMSMS, IonizationType ionizationType) {
    ILipidAnnotation molecularSpeciesLevelAnnotation =
        LIPID_FACTORY.buildMolecularSpeciesLevelLipidFromChains(lipidAnnotation.getLipidClass(),
            predictedChains);
    Double msMsScore =
        calculateMsMsScore(massList, listOfDetectedFragments, minMsMsScore, mzTolRangeMSMS);
    if (msMsScore >= minMsMsScore) {
      return new MatchedLipid(molecularSpeciesLevelAnnotation, accurateMz, ionizationType,
          listOfDetectedFragments, msMsScore);
    }
    return null;
  }


  private boolean checkChainTypesFitLipidClass(List<ILipidChain> chains, ILipidClass lipidClass) {
    List<LipidChainType> lipidClassChainTypes = Arrays.asList(lipidClass.getChainTypes());
    List<LipidChainType> chainTypes =
        chains.stream().map(ILipidChain::getLipidChainType).collect(Collectors.toList());
    Collections.sort(lipidClassChainTypes);
    Collections.sort(chainTypes);
    return lipidClassChainTypes.equals(chainTypes);
  }

  private List<ILipidChain> getChainsFromFragments(Set<LipidFragment> listOfDetectedFragments) {
    LipidChainFactory chainFactory = new LipidChainFactory();
    List<ILipidChain> chains = new ArrayList<>();
    for (LipidFragment lipidFragment : listOfDetectedFragments) {
      if (lipidFragment.getLipidChainType() != null && lipidFragment.getChainLength() != null
          && lipidFragment.getNumberOfDBEs() != null) {
        chains.add(chainFactory.buildLipidChain(lipidFragment.getLipidChainType(),
            lipidFragment.getChainLength(), lipidFragment.getNumberOfDBEs()));
      }
    }
    return chains.stream().distinct().collect(Collectors.toList());
  }

  /**
   * Calculate the explained intensity of MS/MS signals by lipid fragmentation rules in %
   */
  public Double calculateMsMsScore(DataPoint[] massList, Set<LipidFragment> annotatedFragments,
      Double precursor, MZTolerance mzTolRangeMSMS) {
    Double intensityAllSignals = Arrays.stream(massList)
        .filter(dp -> !mzTolRangeMSMS.checkWithinTolerance(dp.getMZ(), precursor))
        .mapToDouble(DataPoint::getIntensity).sum();
    Double intensityMatchedSignals = annotatedFragments.stream().map(LipidFragment::getDataPoint)
        .mapToDouble(DataPoint::getIntensity).sum();
    return (intensityMatchedSignals / intensityAllSignals) * 100;
  }
}
