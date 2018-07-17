/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.testtool;

/**
 *
 * @author tleeuw
 */
public interface StorageEventListener {
    void onStorageAdded(String storageName);
    void onStorageRemoved(String storageName);
}
