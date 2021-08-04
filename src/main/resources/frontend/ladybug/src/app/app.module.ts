import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { DebugComponent } from './debug/debug.component';
import { TestComponent } from './test/test.component';
import { CompareComponent } from './compare/compare.component';
import { TableComponent } from './shared/table/table.component';
import { DisplayComponent } from './shared/display/display.component';
import { TreeComponent } from './shared/tree/tree.component';
import { ButtonComponent } from './shared/button/button.component';
import { DropdownComponent } from './shared/dropdown/dropdown.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

@NgModule({
  declarations: [
    AppComponent,
    DebugComponent,
    TestComponent,
    CompareComponent,
    TableComponent,
    DisplayComponent,
    TreeComponent,
    ButtonComponent,
    DropdownComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    NgbModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
