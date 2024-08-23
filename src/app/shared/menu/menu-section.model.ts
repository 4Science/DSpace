import { MenuItemType } from './menu-item-type.model';
import { AltmetricMenuItemModel } from './menu-item/models/altmetric.model';
import { ExternalLinkMenuItemModel } from './menu-item/models/external-link.model';
import { LinkMenuItemModel } from './menu-item/models/link.model';
import { OnClickMenuItemModel } from './menu-item/models/onclick.model';
import { SearchMenuItemModel } from './menu-item/models/search.model';
import { TextMenuItemModel } from './menu-item/models/text.model';

export type MenuItemModels =
  LinkMenuItemModel
  | AltmetricMenuItemModel
  | ExternalLinkMenuItemModel
  | OnClickMenuItemModel
  | SearchMenuItemModel
  | TextMenuItemModel;

function itemModelFactory(type: MenuItemType): MenuItemModels {
  switch (type) {
    case MenuItemType.TEXT:
      return new TextMenuItemModel();
    case MenuItemType.LINK:
      return new LinkMenuItemModel();
    case MenuItemType.ALTMETRIC:
      return new AltmetricMenuItemModel();
    case MenuItemType.SEARCH:
      return new SearchMenuItemModel();
    case MenuItemType.ONCLICK:
      return new OnClickMenuItemModel();
    case MenuItemType.EXTERNAL:
      return new ExternalLinkMenuItemModel();
    default: {
      throw new Error(`No such menu item type: ${type}`);
    }
  }
}

export interface MenuSection {
  /**
   * The identifier for this section
   */
  id: string;

  /**
   * Whether this section should be visible.
   */
  visible: boolean;

  /**
   *
   */
  model: MenuItemModels;

  /**
   * The identifier of this section's parent section (optional).
   */
  parentID?: string;

  /**
   * The index of this section in its menu.
   */
  index?: number;

  /**
   * Whether this section is currently active.
   * Newly created sections are inactive until toggled.
   */
  active?: boolean;

  /**
   * Whether this section is independent of the route (default: true).
   * This value should be set explicitly for route-dependent sections.
   */
  shouldPersistOnRouteChange?: boolean;


  /**
   * An optional icon for this section.
   * Must correspond to a FontAwesome icon class save for the `.fa-` prefix.
   * Note that not all menus may render icons.
   */
  icon?: string;
}
