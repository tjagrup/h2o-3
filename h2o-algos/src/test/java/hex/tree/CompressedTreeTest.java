package hex.tree;

import hex.genmodel.algos.gbm.GbmMojoModel;
import hex.genmodel.algos.tree.SharedTreeGraph;
import hex.genmodel.algos.tree.SharedTreeSubgraph;
import hex.tree.gbm.GBM;
import hex.tree.gbm.GBMModel;
import org.junit.BeforeClass;
import org.junit.Test;
import water.DKV;
import water.Scope;
import water.TestUtil;
import water.fvec.Frame;

import java.io.IOException;

import static org.junit.Assert.*;

public class CompressedTreeTest extends TestUtil  {

  @BeforeClass()
  public static void setup() { stall_till_cloudsize(1); }

  @Test
  public void testToSharedTreeSubgraph() throws IOException {
    int ntrees = 5;
    try {
      Scope.enter();
      GBMModel model = trainGbm(ntrees);
      GbmMojoModel mojo = (GbmMojoModel) model.toMojo();

      SharedTreeGraph expectedGraph = mojo._computeGraph(-1);
      assertEquals(5, expectedGraph.subgraphArray.size()); // sanity check the MOJO created graph

      for (int i = 0; i < ntrees; i++) {
        CompressedTree tree = model._output._treeKeys[i][0].get();
        assertNotNull(tree);
        CompressedTree auxTreeInfo = model._output._treeKeysAux[i][0].get();
        SharedTreeSubgraph sg = tree.toSharedTreeSubgraph(auxTreeInfo, model._output._names, model._output._domains);

        assertEquals(expectedGraph.subgraphArray.get(i), sg);
      }
    } finally {
      Scope.exit();
    }
  }

  @Test
  public void testMakeTreeKey() {
    try {
      Scope.enter();
      CompressedTree ct = new CompressedTree(new byte[0], 7, 123, 42, 17);
      Scope.track_generic(ct);
      DKV.put(ct);

      CompressedTree.TreeCoords tc = ct.getTreeCoords();
      assertEquals(42, tc._treeId);
      assertEquals(17, tc._clazz);
    } finally {
      Scope.exit();
    }
  }

  private GBMModel trainGbm(final int ntrees) {
    Frame f = Scope.track(parse_test_file("smalldata/logreg/prostate.csv"));

    final String response = "CAPSULE";
    f.replace(f.find(response), f.vec(response).toCategoricalVec()).remove();
    DKV.put(f._key, f);

    GBMModel.GBMParameters gbmParams = new GBMModel.GBMParameters();
    gbmParams._train = f._key;
    gbmParams._ignored_columns = new String[]{"ID"};
    gbmParams._response_column = response;
    gbmParams._ntrees = ntrees;
    gbmParams._score_each_iteration = true;
    return(GBMModel) Scope.track_generic(new GBM(gbmParams).trainModel().get());
  }

}