package titanicsend.model;

import heronarts.lx.transform.LXVector;

import java.util.*;

public class TEBox {
  public List<List<LXVector>> faces;

  public TEBox(List<LXVector> corners) {
    LXVector cornerTLB = corners.get(0);
    LXVector cornerTLF = corners.get(1);
    LXVector cornerTRB = corners.get(2);
    LXVector cornerTRF = corners.get(3);
    LXVector cornerBLB = corners.get(4);
    LXVector cornerBLF = corners.get(5);
    LXVector cornerBRB = corners.get(6);
    LXVector cornerBRF = corners.get(7);

    this.faces = new ArrayList<>();

    List<LXVector> topFace = new ArrayList<>();
    List<LXVector> botFace = new ArrayList<>();
    List<LXVector> leftFace = new ArrayList<>();
    List<LXVector> rightFace = new ArrayList<>();
    List<LXVector> frontFace = new ArrayList<>();
    List<LXVector> backFace = new ArrayList<>();

    topFace.add(cornerTLB);
    topFace.add(cornerTLF);
    topFace.add(cornerTRF);
    topFace.add(cornerTRB);

    botFace.add(cornerBLB);
    botFace.add(cornerBLF);
    botFace.add(cornerBRF);
    botFace.add(cornerBRB);

    leftFace.add(cornerTLB);
    leftFace.add(cornerTLF);
    leftFace.add(cornerBLF);
    leftFace.add(cornerBLB);

    rightFace.add(cornerTRB);
    rightFace.add(cornerTRF);
    rightFace.add(cornerBRF);
    rightFace.add(cornerBRB);

    backFace.add(cornerTLB);
    backFace.add(cornerTRB);
    backFace.add(cornerBRB);
    backFace.add(cornerBLB);

    frontFace.add(cornerTLF);
    frontFace.add(cornerTRF);
    frontFace.add(cornerBRF);
    frontFace.add(cornerBLF);

    this.faces.add(topFace);
    this.faces.add(botFace);
    this.faces.add(leftFace);
    this.faces.add(rightFace);
    this.faces.add(backFace);
    this.faces.add(frontFace);
  }
}
