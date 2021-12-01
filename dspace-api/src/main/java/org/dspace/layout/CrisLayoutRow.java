/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.core.ReloadableEntity;

@Entity
@Table(name = "cris_layout_row")
public class CrisLayoutRow implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cris_layout_row_id_seq")
    @SequenceGenerator(name = "cris_layout_row_id_seq", sequenceName = "cris_layout_row_id_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, insertable = true, updatable = false)
    private Integer id;

    @Column(name = "style")
    private String style;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tab")
    private CrisLayoutTab tab;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "row", cascade = CascadeType.ALL)
    @OrderColumn(name = "position")
    private List<CrisLayoutCell> cells = new ArrayList<>();

    @Override
    public Integer getID() {
        return id;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<CrisLayoutCell> getCells() {
        return cells;
    }

    public void addCell(CrisLayoutCell cell) {
        getCells().add(cell);
        cell.setRow(this);
    }

    public CrisLayoutTab getTab() {
        return tab;
    }

    public void setTab(CrisLayoutTab tab) {
        this.tab = tab;
    }

    public void setCells(List<CrisLayoutCell> cells) {
        this.cells = cells;
    }
}
