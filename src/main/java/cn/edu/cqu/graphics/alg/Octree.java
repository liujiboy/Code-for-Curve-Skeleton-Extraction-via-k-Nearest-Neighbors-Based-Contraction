package cn.edu.cqu.graphics.alg;


import cn.jimmiez.pcu.common.graphics.BoundingBox;
import cn.jimmiez.pcu.common.graphics.Collisions;
import cn.jimmiez.pcu.common.graphics.shape.Box;
import cn.jimmiez.pcu.common.graphics.shape.Sphere;
import cn.jimmiez.pcu.util.PcuArrayUtil;
import cn.jimmiez.pcu.util.PcuCommonUtil;

import javax.vecmath.Point3d;
import java.util.*;


/**
 * An octree can be used to build spatial index for point cloud.
 */
@SuppressWarnings("Duplicates")
public class Octree {

    /**
     * the root node of the octree,
     * the size of root node is the bounding box of point cloud
     **/
    protected OctreeNode root = null;

    protected List<Point3d> points = null;

    /**
     * used to find an octree node by its index
     * the index of an octree node is generated according to its spatial position
     */
    protected Map<Long, OctreeNode> octreeIndices = new HashMap<>();

    /**
     * the max depth of this tree,
     * theoretically it shall be less than 21, because every three bits in a Long-type
     * octree-node index is used to locate its position of siblings.
     **/
    private static final int MAX_DEPTH = 10;

    private static final int MAX_POINTS_PER_NODE = 200;

    /**
     * build spatial index for point cloud
     * note that the length double array in List points must be 3
     * @param points the point cloud
     */
    public void buildIndex(List<Point3d> points) {
        if (points.size() < 1) {
            System.err.println("Warning: input for buildIndex() is an empty list.");
            return;
        }

        this.octreeIndices.clear();
        this.points = points;

        createRootNode();
        createOctree(0, this.root);

    }

    /**
     * If you want to acquire the k-nearest neighbors of a certain point p, call this function,
     * octree will decrease the time cost
     * @param k the number of nearest neighbors
     * @param index the index of point p
     * @return the indices of nearest neighbors
     */
    public int[] searchNearestNeighbors(int k, int index) {
        if (points == null) throw new IllegalStateException("Octree.buildIndex() must be called before searchNearestNeighbors.");
        if (k >= this.points.size()) throw new IllegalArgumentException("number of nearest neighbors is larger than data size");
        return searchNearestNeighbors(k, points.get(index));
    }

    private Comparator<Integer> distanceComparator(final Point3d point, boolean smallFirst) {
        if (smallFirst)
            return new Comparator<Integer>() {
                @Override
                public int compare(Integer pointIndex1, Integer pointIndex2) {
                    Point3d p1 = points.get(pointIndex1);
                    Point3d p2 = points.get(pointIndex2);
                    return Double.compare(p1.distance(point), p2.distance(point));
                }
            };
        return new Comparator<Integer>() {
            @Override
            public int compare(Integer pointIndex1, Integer pointIndex2) {
                Point3d p1 = points.get(pointIndex1);
                Point3d p2 = points.get(pointIndex2);
                return - Double.compare(p1.distance(point), p2.distance(point));
            }
        };
    }

    public int[] searchNearestNeighbors(int k, final Point3d point) {
        if (points == null) throw new IllegalStateException("Octree.buildIndex() must be called before searchNearestNeighbors.");
        if (k >= this.points.size()) throw new IllegalArgumentException("number of nearest neighbors is larger than data size");
        Comparator<Integer> comparator = distanceComparator(point, false);

        long leafNodeIndex = locateOctreeNode(this.root, point);
//        List<Integer> queue = new ArrayList<>();
        PriorityQueue<Integer> queue = new PriorityQueue<>(k, comparator);
        Set<Point3d> set = new HashSet<>(k * 2);
        Set<Long> visitedBoxes = new HashSet<>();
        OctreeNode leafNode = octreeIndices.get(leafNodeIndex);
        double leafSize = leafNode.getxExtent();
        Sphere searchRange = new Sphere(point, leafSize);
        set.add(point);

        while (queue.size() < k) {
            Set<Long> candidates = new HashSet<>();
//            determineCandidatesWithinRadius(currentSearchRadius, point, candidates);
            determineCandidatesWithinRadius(searchRange.getRadius(), searchRange.getCenter(), candidates);
            for (Long newNode : candidates) {
                if (visitedBoxes.contains(newNode)) continue;
                if (Collisions.contains(searchRange, octreeIndices.get(newNode))) visitedBoxes.add(newNode);
                for (int index : octreeIndices.get(newNode).indices) {
                    if (set.contains(points.get(index))) continue;
                    if (points.get(index).distance(point) <= searchRange.getRadius()) {
                        queue.add(index);
                        set.add(points.get(index));
                    }
                    if (queue.size() > k) queue.poll();
                }
//                queue.addAll(octreeIndices.get(newNode).indices);
            }
//            Collections.sort(queue, comparator);
//            while (queue.size() > k) queue.remove(queue.size() - 1);
            searchRange.setRadius(searchRange.getRadius() + leafSize);
        }

        int[] indices = new int[k];
//        System.out.println("" + points.size() + " " + k + " " + queue.size());
        for (int i = 0; i < k; i ++) {
//            indices[i] = queue.get(i);
            indices[i] = queue.poll();
        }
        PcuArrayUtil.reverse(indices);
        return indices;
//        if (points == null) throw new IllegalStateException("Octree.buildIndex() must be called before searchNearestNeighbors.");
//        if (k >= this.points.size()) throw new IllegalArgumentException("number of nearest neighbors is larger than data size");
//        Comparator<Integer> comparator = distanceComparator(point, true);
//
//        long leafNodeIndex = locateOctreeNode(this.root, point);
//        List<Integer> queue = new ArrayList<>();
//        Set<Long> visited = new HashSet<>();
//        OctreeNode leafNode = octreeIndices.get(leafNodeIndex);
////        queue.addAll(leafNode.indices);
//        double leafSize = leafNode.getxExtent();
//        double currentSearchRadius = leafSize;
//
//        while (queue.size() < k) {
//            Set<Long> candidates = new HashSet<>();
//            determineCandidatesWithinRadius(currentSearchRadius, point, candidates);
//            for (Long newNode : candidates) {
//                if (visited.contains(newNode)) continue;
//                visited.add(newNode);
//                queue.addAll(octreeIndices.get(newNode).indices);
//            }
//            Collections.sort(queue, comparator);
//            while (queue.size() > k) queue.remove(queue.size() - 1);
//            currentSearchRadius = currentSearchRadius + leafSize;
//        }
//
//        int[] indices = new int[k];
////        System.out.println("" + points.size() + " " + k + " " + queue.size());
//        for (int i = 0; i < k; i ++) {
//            indices[i] = queue.get(i);
//        }
//        return indices;
    }
    /**
     * determine bounding box of data points, expand the box to make it cubic
     */
    private void createRootNode() {
        Box bbox = BoundingBox.of(points);
        double maxExtent = PcuCommonUtil.max(bbox.getxExtent(), bbox.getyExtent(), bbox.getzExtent());
        this.root = new OctreeNode(bbox.getCenter(), maxExtent, 0);
        this.root.indices = new ArrayList<>(points.size());
        this.root.indices.addAll(PcuCommonUtil.incrementalIntegerList(points.size()));
    }

    /**
     * partition the space recursively
     * @param currentDepth  the depth of current octree node
     * @param currentNode current octree node
     */
    private void createOctree(int currentDepth, OctreeNode currentNode) {
        if (currentNode.indices.size() < 1) return;
        if (currentNode.indices.size() <= MAX_POINTS_PER_NODE || currentDepth >= MAX_DEPTH) {
            this.octreeIndices.put(currentNode.index, currentNode);
            return;
        }
        currentNode.children = new OctreeNode[8];
        int cnt = 0;
        for (int i : new int[]{-1, 1}) {
            for (int j : new int[]{-1, 1}) {
                for (int k : new int[]{-1, 1}) {
                    long index = (long) (((i + 1) * 2 + (j + 1) * 1 + (k + 1) / 2));
//                    index <<= (currentDepth - 1) * 3;
                    index = currentNode.index | (index << (3 * currentDepth + 3));
                    double length = currentNode.getxExtent(); // xExtent == yExtent == zExtent
                    Point3d center = new Point3d(currentNode.getCenter().x + i * length / 2, currentNode.getCenter().y + j * length / 2, currentNode.getCenter().z + k * length / 2);
                    OctreeNode node = new OctreeNode(center, length / 2, currentDepth + 1);
                    currentNode.children[cnt] = node;
                    node.index = index;
                    node.indices = new ArrayList<>(currentNode.indices.size() / 8 + 10);
                    cnt += 1;
                }
            }
        }
        for (int index : currentNode.indices) {
            Point3d point = points.get(index);
            Point3d center = currentNode.getCenter();
            int xi = point.x < center.x ? 0 : 1;
            int yj = point.y < center.y ? 0 : 1;
            int zk = point.z < center.z ? 0 : 1;
            int childIndex = xi * 4 + yj * 2 + zk * 1;
            currentNode.children[childIndex].indices.add(index);
        }
        currentNode.indices = null;
        for (OctreeNode node : currentNode.children) {
            createOctree(currentDepth + 1, node);
        }
    }


    /**
     * find the index of octree node in which the target point is located
     * @param node the root octree node
     * @param point the target point
     * @return the index of leaf node
     */
    protected Long locateOctreeNode(OctreeNode node, Point3d point) {
        if (node.children == null) {
            if (node.contains(point)) {
                return node.index;
            } else {
                throw new IllegalStateException("Search a point exceeding octree bounds.");
            }
        } else {
            int xi = point.x < node.getCenter().x ? 0 : 1;
            int yj = point.y < node.getCenter().y ? 0 : 1;
            int zk = point.z < node.getCenter().z ? 0 : 1;
            int childIndex = xi * 4 + yj * 2 + zk * 1;
            return locateOctreeNode(node.children[childIndex], point);
        }
    }


    private PriorityQueue<Integer> searchNeighborsInNodes(List<Long> candidateLeaves, final Point3d point) {
        int capacity = 0;
        for (long leafIndex : candidateLeaves) capacity += this.octreeIndices.get(leafIndex).indices.size();
        PriorityQueue<Integer> queue = new PriorityQueue<>(capacity, new Comparator<Integer>() {
            @Override
            public int compare(Integer pointIndex1, Integer pointIndex2) {
                Point3d p1 = points.get(pointIndex1);
                Point3d p2 = points.get(pointIndex2);
                return Double.compare(p1.distance(point), p2.distance(point));
            }
        });
        for (long leafIndex : candidateLeaves) {
            queue.addAll(this.octreeIndices.get(leafIndex).indices);
        }
        return queue;
    }

    public List<Integer> searchNeighborsInSphere(Point3d point, double radius) {
        List<Integer> neighborIndices = new ArrayList<>();
        List<Long> candidateLeaves = new ArrayList<>();
        determineCandidatesWithinRadius(radius, point, candidateLeaves);

        PriorityQueue<Integer> queue = searchNeighborsInNodes(candidateLeaves, point);

        while (queue.size() > 0) {
            Integer nextIndex = queue.poll();
            Point3d neighboringPoint = points.get(nextIndex);
            if (point.distance(neighboringPoint) >= radius) {
                break;
            } else {
                neighborIndices.add(nextIndex);
            }
        }
        return neighborIndices;

    }

    /**
     * @param index the index of a point
     * @param radius radius of neighborhood
     * @return indices of neighboring points of this point
     */
    public List<Integer> searchNeighborsInSphere(int index, double radius) {
        return searchNeighborsInSphere(points.get(index), radius);
    }

    private void determineCandidatesWithinRadius(double radius, Point3d point, Collection<Long> candidates) {
        Sphere sphere = new Sphere(point, radius);
        // ===========================================
        // all octree nodes that intersects with sphere will be added into queue
        List<OctreeNode> visitingQueue = new ArrayList<>();
        if (Collisions.intersect(root, sphere)) visitingQueue.add(root);
        int currentVisit = 0;
        for (; currentVisit < visitingQueue.size(); currentVisit ++) {
            OctreeNode visiting = visitingQueue.get(currentVisit);
            if (visiting.isLeaf()) {
                if (octreeIndices.get(visiting.index) == null) continue;
                candidates.add(visiting.index);
            } else {
                for (OctreeNode child : visiting.children) {
                    if (Collisions.intersect(child, sphere)) {
                        visitingQueue.add(child);
                    }
                }
            }
        }

    }


    public class OctreeNode extends Box {

        /**
         * default value is root index
         * the index is generated in createOctree()
         **/
        Long index = 0L;

        /**
         * an octree node holds the indices of 3d points in the List
         * in a non-leaf node, field indices is null
         **/
        List<Integer> indices = null;

        /** in a non-leaf node, field indices is null **/
        OctreeNode[] children = null;

        int depth = 0;

        OctreeNode(Point3d center, double length, int depth) {
            this.center = new Point3d(center);
            this.xExtent = length;
            this.yExtent = length;
            this.zExtent = length;
            this.depth = depth;
        }

        public List<Integer> getIndices() {
            return indices;
        }

        public OctreeNode[] getChildren() {
            return children;
        }

        public int getDepth() {
            return depth;
        }

        public boolean isLeaf() {
            return children == null;
        }
    }

}

