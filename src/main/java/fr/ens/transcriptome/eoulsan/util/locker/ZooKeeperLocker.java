/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.util.locker;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

/**
 * This class implements a locker using Zookeeper.
 * @see https
 *      ://www.altamiracorp.com/blog/employee-posts/distributed-lock-using-
 *      zookeeper
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ZooKeeperLocker implements Locker, Watcher {

  private final ZooKeeper zk;
  private final String lockBasePath;
  private final String lockName;

  private String lockPath;
  private boolean response;

  @Override
  public void lock() throws IOException {

    // Test if the connection is alive
    if (!this.response) {
      throw new IOException("Connection to Zookeeper is not alive");
    }

    try {

      if (this.zk.exists(this.lockBasePath, false) == null) {

        this.zk.create(this.lockBasePath, new byte[0],
            ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      }

      // lockPath will be different than (lockBasePath + "/" + lockName) because
      // of the sequence number ZooKeeper appends

      this.lockPath =
          this.zk.create(this.lockBasePath + "/" + this.lockName, null,
              Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
      final Object lock = new Object();

      synchronized (lock) {

        while (true) {

          List<String> nodes =
              this.zk.getChildren(this.lockBasePath, new Watcher() {

                @Override
                public void process(WatchedEvent event) {

                  synchronized (lock) {
                    lock.notifyAll();
                  }
                }
              });

          Collections.sort(nodes);

          if (this.lockPath.endsWith(nodes.get(0))) {
            return;
          }

          lock.wait();
        }
      }
    } catch (KeeperException e) {
      throw new IOException(e);
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void unlock() throws IOException {

    try {
      this.zk.delete(this.lockPath, -1);
      this.lockPath = null;
      this.zk.close();
      this.response = false;
    } catch (KeeperException e) {
      throw new IOException(e);
    } catch (InterruptedException e) {
      throw new IOException(e);
    }

  }

  //
  // Watcher method
  //

  @Override
  public void process(final WatchedEvent event) {

    if (event.getState() == KeeperState.SyncConnected) {
      this.response = true;
    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @throws IOException if an error occurs while creating the ZooKeeper
   *           connection
   */
  public ZooKeeperLocker(final String connectString, final int sessionTimeout,
      final String lockBasePath, final String lockName) throws IOException {

    this.lockBasePath = lockBasePath;
    this.lockName = lockName;

    this.zk = new ZooKeeper(connectString, sessionTimeout, this);

    // Try to connect to ZooKeeper server in the next 30 seconds
    int count = 0;
    while (!this.response) {

      if (count > 30) {
        throw new IOException("Unable to connect to Zookeeper");
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new IOException(e.getMessage());
      }

      count++;
    }

  }

}